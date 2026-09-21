package io.arpicode.leagueapi;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * A full-context test, driving the HTTP API through {@link org.springframework.test.web.servlet.MockMvc}
 * against the shared Postgres container.
 *
 * <p>Every {@code @SpringBootTest} in this suite must declare the <em>identical</em> configuration
 * to avoid starting a new full context.
 *
 * <p>{@link ThrowingEndpointConfiguration} is therefore part of the shared configuration even though
 * only {@code ErrorContractTest} exercises the endpoint it registers.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@Import({TestcontainersConfiguration.class, ThrowingEndpointConfiguration.class})
@AutoConfigureMockMvc
public @interface ApiIntegrationTest {
}
