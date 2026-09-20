package io.arpicode.leagueapi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // Not a leak, so do not wrap this in try-with-resources: Spring Boot's
    // TestcontainersLifecycleBeanPostProcessor starts the container and stops it when the
    // context closes. Closing it here would stop it before the first test ran.
    @Bean
    @ServiceConnection
    @SuppressWarnings("resource")
    public PostgreSQLContainer postgresContainer() {
        // Must mirror POSTGRES_INITDB_ARGS in docker-compose.yml: without it the test
        // database collates by byte value while a dev or prod database pinned to ICU
        // does not, and ordering assertions would only hold in one of them.
        return new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
                .withEnv("POSTGRES_INITDB_ARGS", "--locale-provider=icu --icu-locale=und --encoding=UTF8");
    }

}
