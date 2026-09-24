package io.arpicode.leagueapi.shared.error;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

// These cover the one part of the handler that a real request cannot reach, the walk up
// the cause chain in findConstraintViolation.
//
// Everything the handler does for a real request stays in ErrorContractTest.
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("should map the constraint when the violation is buried in the cause chain")
    void mapsConstraintFoundDeepInTheCauseChain() {
        // Narrowing the walk to the immediate cause degrades this to a generic 409, which is the
        // regression an extra wrapper (a commit-time RollbackException) would cause in production.
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement",
                new IllegalStateException("commit failed", constraintViolation("uq_player_username_normalized")));

        ProblemDetail problem = handler.handleDataIntegrity(ex);

        assertThat(problem.getProperties()).containsEntry("code", ErrorCode.USERNAME_ALREADY_EXISTS.name());
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getDetail()).isEqualTo(UserMessages.USERNAME_ALREADY_EXISTS);
    }

    @Test
    @DisplayName("should fall back to a generic conflict when the constraint has no mapping")
    void fallsBackToGenericConflictForAnUnmappedConstraint() {
        // A constraint added to the schema but not to CONSTRAINT_MAPPINGS must still reach the
        // client as a 409 carrying the contract, not leak the constraint name or become a 500.
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement", constraintViolation("uq_some_future_constraint"));

        ProblemDetail problem = handler.handleDataIntegrity(ex);

        assertThat(problem.getProperties()).containsEntry("code", ErrorCode.DATA_INTEGRITY_VIOLATION.name());
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getDetail()).isEqualTo(UserMessages.CONFLICT);
    }

    @Test
    @DisplayName("should fall back to a generic conflict when no constraint violation is in the chain")
    void fallsBackToGenericConflictWhenNoConstraintViolationIsPresent() {
        // Dropping the null guard on the violation turns this into an NPE, which the catch-all
        // handler would then answer with a 500 instead of the 409 the client expects.
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("could not execute statement");

        ProblemDetail problem = handler.handleDataIntegrity(ex);

        assertThat(problem.getProperties()).containsEntry("code", ErrorCode.DATA_INTEGRITY_VIOLATION.name());
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("should map a failed version check to a conflict rather than an internal error")
    void mapsOptimisticLockFailureToConflict() {
        // Losing a @Version race is an ordinary concurrency outcome, not a server fault. Without
        // its own handler this falls to the catch-all and the client is told 500 INTERNAL_ERROR
        // for a request it could simply retry.
        ProblemDetail problem = handler.handleOptimisticLock(
                new OptimisticLockingFailureException("Row was updated or deleted by another transaction"));

        assertThat(problem.getProperties()).containsEntry("code", ErrorCode.CONCURRENT_MODIFICATION.name());
        assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(problem.getDetail()).isEqualTo(UserMessages.CONCURRENT_MODIFICATION);
    }

    private static ConstraintViolationException constraintViolation(String constraintName) {
        return new ConstraintViolationException(
                "duplicate key value violates unique constraint",
                new SQLException("duplicate key", "23505"),
                constraintName);
    }

}
