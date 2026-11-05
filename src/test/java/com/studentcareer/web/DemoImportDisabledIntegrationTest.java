package com.studentcareer.web;

import com.studentcareer.company.importing.DemoCompanyImportService;
import com.studentcareer.support.TestDatabaseSafetyInitializer;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.demo.enabled=false")
class DemoImportDisabledIntegrationTest {
    @LocalServerPort int port;
    @Autowired ApplicationContext context;
    @Autowired JdbcTemplate jdbc;

    @Test
    void shouldDisableFictionalImportWithoutModifyingPersistedData() throws Exception {
        assertTrue(context.getBeansOfType(DemoCompanyImportService.class).isEmpty());
        long before = jdbc.queryForObject("SELECT count(*) FROM company_dossiers", Long.class);
        try (var client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/imports/demo"))
                    .timeout(Duration.ofSeconds(10)).POST(HttpRequest.BodyPublishers.noBody()).build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(404, response.statusCode(), response.body());
        }
        assertEquals(before, jdbc.queryForObject("SELECT count(*) FROM company_dossiers", Long.class));
    }
}
