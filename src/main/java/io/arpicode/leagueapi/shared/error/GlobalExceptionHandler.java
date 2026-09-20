package io.arpicode.leagueapi.shared.error;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
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
    private static final String ERROR_ID = "errorId";
    private static final String ERRORS = "errors";

    private static final Map<String, ConstraintMapping> CONSTRAINT_MAPPINGS = Map.of(
            "uq_player_username_normalized", new ConstraintMapping(ErrorCode.USERNAME_ALREADY_EXISTS, UserMessages.USERNAME_ALREADY_EXISTS),
            "uq_player_email", new ConstraintMapping(ErrorCode.EMAIL_ALREADY_EXISTS, UserMessages.EMAIL_ALREADY_EXISTS),
            "uq_board_game_name_normalized", new ConstraintMapping(ErrorCode.BOARD_GAME_NAME_ALREADY_EXISTS, UserMessages.BOARD_GAME_NAME_ALREADY_EXISTS)
    );

    private record ConstraintMapping(ErrorCode code, String message) {
    }

    public record FieldViolation(String field, String message) {
    }

    // Business rules violations
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException ex) {
        ProblemDetail problem = problem(ex.getCode(), ex.getMessage());
        log.info("[{}] business error {}: {}", errorIdOf(problem), ex.getCode(), ex.getMessage());

        return problem;
    }

    // Database constraint violations
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        ConstraintViolationException violation = findConstraintViolation(ex);
        String constraintName = violation == null ? null : violation.getConstraintName();
        ConstraintMapping mapping = constraintName == null ? null : CONSTRAINT_MAPPINGS.get(constraintName);

        if (mapping != null) {
            ProblemDetail problem = problem(mapping.code(), mapping.message());
            log.info("[{}] constraint {} violated", errorIdOf(problem), constraintName);

            return problem;
        }

        // Unmapped constraint in ConstraintMapping: the client receives a generic 409, the technical detail remains here.
        ProblemDetail problem = problem(ErrorCode.DATA_INTEGRITY_VIOLATION, UserMessages.CONFLICT);
        log.warn("[{}] unmapped data integrity violation (constraint={})", errorIdOf(problem), constraintName, ex);

        return problem;
    }

    // Anything neither handled above nor by ResponseEntityExceptionHandler.
    // Without this, unhandled exceptions fall through to Boot's BasicErrorController,
    // which answers with a different JSON shape carrying no code and no errorId.
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        ProblemDetail problem = problem(ErrorCode.INTERNAL_ERROR, UserMessages.INTERNAL_ERROR);
        log.error("[{}] unexpected error", errorIdOf(problem), ex);

        return problem;
    }

    // Request validation errors (e.g. @Valid)
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
        log.info("[{}] validation failed: {}", errorIdOf(problem), violations);

        return ResponseEntity.status(status).headers(headers).body(problem);
    }

    // Every ProblemDetail built by the inherited handlers (malformed JSON, bad path
    // variable, unknown route, wrong method, unsupported media type) passes through
    // here, so they carry the same code/errorId contract as the ones built above.
    @Override
    protected @NonNull ResponseEntity<Object> createResponseEntity(
            Object body,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode statusCode,
            @NonNull WebRequest request) {
        if (body instanceof ProblemDetail problem && !hasCode(problem)) {
            ErrorCode code = codeForStatus(statusCode);
            decorate(problem, code);
            log.info("[{}] request rejected with {}: {}", errorIdOf(problem), statusCode.value(), code);
        }

        return super.createResponseEntity(body, headers, statusCode, request);
    }

    // Helper methods

    private static ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(statusOf(code), detail);
        decorate(problem, code);

        return problem;
    }

    private static void decorate(ProblemDetail problem, ErrorCode code) {
        problem.setProperty(CODE, code.name());
        problem.setProperty(ERROR_ID, UUID.randomUUID().toString());
    }

    private static boolean hasCode(ProblemDetail problem) {
        Map<String, Object> properties = problem.getProperties();

        return properties != null && properties.containsKey(CODE);
    }

    private static String errorIdOf(ProblemDetail problem) {
        Map<String, Object> properties = problem.getProperties();

        return properties == null ? "-" : String.valueOf(properties.get(ERROR_ID));
    }

    // Walks the cause chain instead of inspecting only the immediate cause: any extra
    // wrapper (a custom PersistenceExceptionTranslator, a commit-time RollbackException)
    // would otherwise hide the constraint name and degrade duplicates to a generic 409.
    private static ConstraintViolationException findConstraintViolation(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation;
            }
            if (cause.getCause() == cause) {
                break;
            }
        }

        return null;
    }

    private static HttpStatus statusOf(ErrorCode code) {
        return switch (code) {
            case PLAYER_NOT_FOUND,
                    NOT_FOUND,
                    BOARD_GAME_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case USERNAME_ALREADY_EXISTS,
                    BOARD_GAME_NAME_ALREADY_EXISTS,
                    EMAIL_ALREADY_EXISTS,
                    DATA_INTEGRITY_VIOLATION -> HttpStatus.CONFLICT;
            case VALIDATION_ERROR, MALFORMED_REQUEST -> HttpStatus.BAD_REQUEST;
            case METHOD_NOT_ALLOWED -> HttpStatus.METHOD_NOT_ALLOWED;
            case UNSUPPORTED_MEDIA_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
            case INTERNAL_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static ErrorCode codeForStatus(HttpStatusCode status) {
        if (status.isSameCodeAs(HttpStatus.NOT_FOUND)) {
            return ErrorCode.NOT_FOUND;
        }
        if (status.isSameCodeAs(HttpStatus.METHOD_NOT_ALLOWED)) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        if (status.isSameCodeAs(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
            return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        }

        return status.is4xxClientError() ? ErrorCode.MALFORMED_REQUEST : ErrorCode.INTERNAL_ERROR;
    }

}
