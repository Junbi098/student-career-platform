package com.studentcareer.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import com.studentcareer.support.TestDatabaseSafetyInitializer;
import com.studentcareer.support.TestDatabaseCleanup;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CompanyApiIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetIsolatedTestDatabase() {
        TestDatabaseCleanup.clear(jdbc);
    }

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper mapper = new ObjectMapper();

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(10));
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode get(String path) throws Exception {
        var response = request("GET", path, null);
        assertEquals(200, response.statusCode(), response.body());
        return mapper.readTree(response.body());
    }

    @Test
    void shouldImportSearchPaginateAndReadDossiersOverRealHttp() throws Exception {
        assertEquals(0, get("/api/companies").path("totalElements").asInt());
        assertEquals("POSTGRESQL", get("/api/status").path("storage").asText());
        var imported = request("POST", "/api/imports/demo", null);
        assertEquals(200, imported.statusCode(), imported.body());
        JsonNode report = mapper.readTree(imported.body());
        assertEquals("FICTIONAL_DEMO", report.path("mode").asText());
        assertEquals(4, report.path("total").asInt());
        assertEquals(2, report.path("summary").path("SUCCESS").asInt());
        assertEquals(1, report.path("summary").path("POSSIBLE_DUPLICATE").asInt());
        assertEquals(1, report.path("summary").path("NO_COMPANY").asInt());
        JsonNode page = get("/api/companies?size=2");
        assertEquals(3, page.path("totalElements").asInt());
        assertEquals(2, page.path("totalPages").asInt());
        assertEquals(2, page.path("items").size());
        String id = page.path("items").get(0).path("id").asText();
        JsonNode detail = get("/api/companies/" + id);
        assertEquals("Island Digital Studio", detail.path("company").path("name").asText());
        assertEquals("FIXTURE", detail.path("source").path("kind").asText());
        assertEquals("classpath:/demo/company-complete.html", detail.path("source").path("url").asText());
        assertDoesNotThrow(() -> Instant.parse(detail.path("source").path("accessedAt").asText()));
        assertEquals("POSSIBLE_DUPLICATE", page.path("items").get(1).path("importStatus").asText());
        assertEquals(detail.path("source").path("url").asText(), page.path("items").get(1).path("duplicateOf").asText());
        assertEquals(2, get("/api/companies?q=ISLAND&industry=software&location=EBENE").path("totalElements").asInt());
        assertEquals(1, get("/api/companies?q=lagoon").path("totalElements").asInt());
        assertEquals(0, get("/api/companies?q=unknown-company").path("totalElements").asInt());
        assertEquals(1, get("/api/companies?page=1&size=2").path("items").size());
        assertEquals(0, get("/api/companies?page=2147483647&size=100").path("items").size());
        assertEquals(200, request("POST", "/api/imports/demo", null).statusCode());
        var reimported = get("/api/companies");
        assertEquals(3, reimported.path("totalElements").asInt());
        assertEquals(id, reimported.path("items").get(0).path("id").asText());
    }

    @Test
    void shouldReturnProblemDetailsForInvalidRequestsAndMissingDossiers() throws Exception {
        for (String path : new String[]{"/api/companies?size=101", "/api/companies?size=0",
                "/api/companies?page=-1", "/api/companies?page=abc", "/api/companies/not-a-uuid",
                "/api/companies?q=" + "x".repeat(201)}) {
            var response = request("GET", path, null);
            assertEquals(400, response.statusCode(), path + ": " + response.body());
            assertEquals(400, mapper.readTree(response.body()).path("status").asInt());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/problem+json"));
            assertFalse(response.body().contains("stackTrace"));
        }
        var unknown = request("GET", "/api/companies/00000000-0000-0000-0000-000000000000", null);
        assertEquals(404, unknown.statusCode());
        assertEquals(404, mapper.readTree(unknown.body()).path("status").asInt());
        assertEquals(404, request("GET", "/api/not-a-route", null).statusCode());
    }

    @Test
    void shouldNotAcceptArbitraryFilePathsOrWebUrisThroughDemoImport() throws Exception {
        var response = request("POST", "/api/imports/demo", mapper.writeValueAsString(java.util.Map.of("uri", "file:///private.html")));
        assertEquals(400, response.statusCode(), response.body());
        assertEquals(405, request("GET", "/api/imports/demo", null).statusCode());
        assertEquals(405, request("POST", "/api/companies", "{}").statusCode());
    }
    @Test
    void shouldExposeSourceRegistryAndRejectArbitraryInputWithoutNetworkCollection() throws Exception {
        JsonNode registry = get("/api/sources");
        assertEquals("CURATED_SEEDS", registry.path("discoveryMode").asText());
        assertEquals("UNVERIFIED", registry.path("verificationStatus").asText());
        assertEquals(3, registry.path("sources").size());
        assertEquals("CC0-1.0", registry.path("sources").get(0).path("license").asText());
        var response = request("POST", "/api/imports/wikidata", mapper.writeValueAsString(java.util.Map.of("url", "http://localhost/private")));
        assertEquals(400, response.statusCode());
        assertEquals(405, request("GET", "/api/imports/wikidata", null).statusCode());
    }
}
