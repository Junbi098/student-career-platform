
package com.studentcareer.poc.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.poc.CompanyHtmlParser;
import com.studentcareer.poc.domain.model.SourceReference;
import com.studentcareer.poc.fetch.LocalFileSourceFetcher;
import com.studentcareer.poc.output.CompanyDossierJsonWriter;
import com.studentcareer.poc.service.CompanyIntelligenceService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CompanyDossierPipelineTest {

    private final CompanyDossierPipeline pipeline =
            new CompanyDossierPipeline(
                    new CompanyIntelligenceService(
                            new LocalFileSourceFetcher(),
                            new CompanyHtmlParser()
                    ),
                    new CompanyDossierJsonWriter()
            );

    private final ObjectMapper mapper = new ObjectMapper();

    private SourceReference createFictionalSource() {
        return new SourceReference(
                URI.create("https://example.com/fictional-company"),
                "Fictional test source - not fetched",
                Instant.parse("2026-10-27T12:00:00Z")
        );
    }

    @Test
    void shouldGenerateJsonFromLocalHtml() throws Exception {

        URL resource = getClass().getResource(
                "/fixtures/company-complete.html"
        );

        assertNotNull(resource, "HTML fixture not found");

        Optional<String> result = pipeline.execute(
                resource.toURI(),
                createFictionalSource()
        );

        assertTrue(result.isPresent());

        JsonNode root = mapper.readTree(result.orElseThrow());

        assertEquals(
                "Island Digital Studio",
                root.path("company").path("name").asText()
        );

        assertEquals(
                "Software Development",
                root.path("company").path("industry").asText()
        );

        assertEquals(
                "Ebene, Mauritius",
                root.path("company").path("location").asText()
        );

        assertEquals(
                "https://example.com",
                root.path("company").path("website").asText()
        );

        assertEquals(
                "Fictional test source - not fetched",
                root.path("source").path("title").asText()
        );
    }

    @Test
    void shouldReturnEmptyWhenNameIsMissing() throws Exception {

        Path file = Files.createTempFile(
                "company-no-name-", ".html"
        );

        try {
            Files.writeString(
                    file,
                    "<p class='company-industry'>Technology</p>"
            );

            Optional<String> result = pipeline.execute(
                    file.toUri(),
                    createFictionalSource()
            );

            assertTrue(result.isEmpty());

        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void shouldPropagateMissingFileError() {

        URI missingFile = URI.create(
                "file:///nonexistent-studentcareer-pipeline-12345.html"
        );

        assertThrows(
                IOException.class,
                () -> pipeline.execute(
                        missingFile,
                        createFictionalSource()
                )
        );
    }
}
