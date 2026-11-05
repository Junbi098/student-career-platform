package com.studentcareer.support;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import static org.junit.jupiter.api.Assertions.*;

class TestDatabaseSafetyInitializerTest {
    @Test
    void shouldRejectApplicationDatabaseBeforeBeanInitialization() {
        try (var context = new GenericApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("spring.datasource.url", "jdbc:postgresql://127.0.0.1:15432/student_career")));
            assertThrows(IllegalStateException.class, () -> new TestDatabaseSafetyInitializer().initialize(context));
        }
    }

    @Test
    void shouldAcceptExplicitlyNamedPostgreSqlTestDatabase() {
        try (var context = new GenericApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("test",
                    Map.of("spring.datasource.url", "jdbc:postgresql://127.0.0.1:15432/student_career_test")));
            assertDoesNotThrow(() -> new TestDatabaseSafetyInitializer().initialize(context));
        }
    }
}
