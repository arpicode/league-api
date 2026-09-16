package io.arpicode.leagueapi.shared.error;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CODE = "code";
    private static final String TRACE_ID = "traceId";
    private static final String ERRORS = "errors";

    // Each key in this map represents the name of a database constraint exactly as Postgres defines it.
    // NOTE: migrations must be updated to ensure that the constraint names match the keys in this map.
    private static final Map<String, ConstraintMapping> CONSTRAINT_MAPPINGS = Map.of(
            "player_username_key", new ConstraintMapping(ErrorCode.USERNAME_ALREADY_EXISTS, UserMessages.USERNAME_ALREADY_EXISTS),
            "player_email_key", new ConstraintMapping(ErrorCode.EMAIL_ALREADY_EXISTS, UserMessages.EMAIL_ALREADY_EXISTS));

    private record ConstraintMapping(ErrorCode code, String message) {
    }

    public record FieldViolation(String field, String message) {
    }

    //-- Business rules violations
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        ProblemDetail problem = problem(ex.getCode(), ex.getMessage());
        log.debug("[{}] business error {}: {}", traceIdOf(problem), ex.getCode(), ex.getMessage());

        return problem;
    }

    //-- Database constraint violations
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        String constraintName = ex.getCause() instanceof org.hibernate.exception.ConstraintViolationException cve
                ? cve.getConstraintName()
                : null;
        ConstraintMapping mapping = constraintName == null ? null : CONSTRAINT_MAPPINGS.get(constraintName);

        if (mapping != null) {
            ProblemDetail problem = problem(mapping.code(), mapping.message());
            log.debug("[{}] constraint {} violated", traceIdOf(problem), constraintName);

            return problem;
        }

        // Unmapped constraint in ConstraintMapping: the client receives a generic 409, the technical detail remains here.
        ProblemDetail problem = problem(ErrorCode.DATA_INTEGRITY_VIOLATION, UserMessages.CONFLICT);
        log.warn("[{}] unmapped data integrity violation (constraint={})", traceIdOf(problem), constraintName, ex);

        return problem;
    }

    //-- Request validation errors (e.g. @Valid)
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .sorted(Comparator.comparing(FieldViolation::field)
                        .thenComparing(FieldViolation::message, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        ProblemDetail problem = problem(ErrorCode.VALIDATION_ERROR, UserMessages.VALIDATION_ERROR);
        problem.setProperty(ERRORS, violations);
        log.debug("[{}] validation failed: {}", traceIdOf(problem), violations);

        return ResponseEntity.status(status).headers(headers).body(problem);
    }

    //-- Helper methods

    private static ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusOf(code), detail);
        problem.setProperty(CODE, code.name());
        problem.setProperty(TRACE_ID, UUID.randomUUID().toString());

        return problem;
    }

    private static String traceIdOf(ProblemDetail problem) {
        Map<String, Object> properties = problem.getProperties();

        return properties == null ? "-" : String.valueOf(properties.get(TRACE_ID));
    }

    private static HttpStatus statusOf(ErrorCode code) {
        return switch (code) {
            case PLAYER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case USERNAME_ALREADY_EXISTS, EMAIL_ALREADY_EXISTS, DATA_INTEGRITY_VIOLATION -> HttpStatus.CONFLICT;
            case VALIDATION_ERROR -> HttpStatus.BAD_REQUEST;
        };
    }

}
