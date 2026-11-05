package com.studentcareer.support;

import java.net.URI;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** Runs before JPA/Flyway beans, preventing tests from migrating or cleaning an application database. */
public class TestDatabaseSafetyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String url = context.getEnvironment().getRequiredProperty("spring.datasource.url");
        if (!url.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("Integration tests require an isolated PostgreSQL test database");
        }
        String path = URI.create(url.substring("jdbc:".length())).getPath();
        if (path == null || !path.endsWith("_test")) {
            throw new IllegalStateException("Refusing to run tests against a database without the _test suffix");
        }
    }
}
