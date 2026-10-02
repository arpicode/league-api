package io.arpicode.leagueapi;

import io.arpicode.leagueapi.boardgame.BoardGameFixtures;
import io.arpicode.leagueapi.player.PlayerFixtures;
import io.arpicode.leagueapi.tournament.TournamentFixtures;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A full-context test, driving the HTTP API through {@link org.springframework.test.web.servlet.MockMvc}
 * against the shared Postgres container.
 *
 * <p>Every {@code @SpringBootTest} in this suite must declare the <em>identical</em> configuration
 * to avoid starting a new full context.
 *
 * <p>{@link ThrowingEndpointConfiguration} is therefore part of the shared configuration even though
 * only {@code ErrorContractTest} exercises the endpoint it registers. The fixtures are imported here
 * for the same reason: a test class that imported one itself would get a context of its own.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@Import({
    TestcontainersConfiguration.class,
    ThrowingEndpointConfiguration.class,
    PlayerFixtures.class,
    BoardGameFixtures.class,
    TournamentFixtures.class
})
@AutoConfigureMockMvc
public @interface ApiIntegrationTest {
}
