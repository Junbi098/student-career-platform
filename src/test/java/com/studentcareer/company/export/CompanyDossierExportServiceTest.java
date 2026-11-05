package com.studentcareer.company.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.company.export.CompanyDossierJsonWriter;
import com.studentcareer.company.parsing.CompanyHtmlParser;
import com.studentcareer.company.service.CompanyAnalysisService;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.reader.LocalFileSourceReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompanyDossierExportServiceTest {

    private final CompanyDossierExportService pipeline =
            new CompanyDossierExportService(
                    new CompanyAnalysisService(
                            new LocalFileSourceReader(),
                            new CompanyHtmlParser()
                    ),
                    new CompanyDossierJsonWriter()
            );

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldGenerateJsonFromLocalHtml() throws Exception {

        URL resource = getClass().getResource(
                "/demo/company-complete.html"
        );

        assertNotNull(resource, "HTML fixture not found");

        Optional<String> result = pipeline.execute(
                resource.toURI(),
                SourceKind.FIXTURE
        );

        assertTrue(result.isPresent());

        JsonNode root = mapper.readTree(result.orElseThrow());
        assertEquals("FIXTURE", root.path("source").path("kind").asText());
        assertEquals(Path.of(resource.toURI()).toRealPath().toUri().toString(),
                root.path("source").path("url").asText());
        assertDoesNotThrow(() -> java.time.Instant.parse(root.path("source").path("accessedAt").asText()));

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
                "Fictional local fixture: company-complete.html",
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
                    SourceKind.FIXTURE
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
                        SourceKind.FIXTURE
                )
        );
    }
    @Test
    void shouldRejectWebInputWithoutClaimingItWasRead() {
        assertThrows(IllegalArgumentException.class, () -> pipeline.execute(
                URI.create("https://example.com/not-read"), SourceKind.LOCAL_DOCUMENT));
    }
}
