package io.arpicode.leagueapi;

import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

/**
 * Runs an {@link ApiIntegrationTest} method outside the rolled-back test transaction, so every
 * request commits on its own as it does in production, then empties every league table.
 *
 * <p>Only for behavior that a commit decides: {@code updatedAt} moving between two requests,
 * a constraint violation raised against a committed row, a DELETE that must reach Postgres.
 * Everything else stays in the class's rolled-back transaction.
 *
 * <p>The cleanup truncates by table rather than deleting by name, so these tests can use fixture
 * defaults like any other. Committed rows left behind would break the list tests'
 * {@code totalElements}; a new table therefore goes into {@code truncate-league-tables.sql}.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Sql(scripts = "/sql/truncate-league-tables.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public @interface CommitsData {
}
