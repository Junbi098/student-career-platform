package com.studentcareer.support;

import org.springframework.jdbc.core.JdbcTemplate;

public final class TestDatabaseCleanup {
    private TestDatabaseCleanup() { }

    public static void clear(JdbcTemplate jdbc) {
        String database = jdbc.queryForObject("SELECT current_database()", String.class);
        if (database == null || !database.endsWith("_test")) {
            throw new IllegalStateException("Refusing to clean a non-test database");
        }
        jdbc.update("DELETE FROM company_dossiers");
        jdbc.update("DELETE FROM source_references");
    }
}
