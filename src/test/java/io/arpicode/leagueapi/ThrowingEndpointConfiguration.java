package io.arpicode.leagueapi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * An endpoint that fails the way a real bug would, so the catch-all handler has something to catch.
 *
 * <p>Shared rather than nested in a single test class on purpose: a nested {@code @TestConfiguration}
 * is auto-registered on top of the primary configuration, so each test class that declares its own
 * copy ends up with a distinct merged configuration. Distinct configurations mean distinct context
 * cache keys, which means a second application context and a second {@code @ServiceConnection}
 * Postgres container. Importing one shared class from every test class keeps the key identical and
 * the whole suite on a single context.
 */
@TestConfiguration(proxyBeanMethods = false)
public class ThrowingEndpointConfiguration {

    @RestController
    static class ThrowingEndpoint {
        @GetMapping("/api/v1/test-unexpected-error")
        void boom() {
            throw new IllegalStateException("simulated unexpected failure");
        }
    }

}
