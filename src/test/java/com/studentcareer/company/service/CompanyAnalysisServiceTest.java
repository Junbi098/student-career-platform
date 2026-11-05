package com.studentcareer.company.service;

import com.studentcareer.company.domain.Company;
import com.studentcareer.company.parsing.CompanyHtmlParser;
import com.studentcareer.source.reader.LocalFileSourceReader;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompanyAnalysisServiceTest {

    private final CompanyAnalysisService service =
            new CompanyAnalysisService(
                    new LocalFileSourceReader(),
                    new CompanyHtmlParser()
            );

    @Test
    void shouldAnalyzeLocalCompanyPage() throws Exception {

        URL resource = getClass().getResource(
                "/demo/company-complete.html"
        );

        assertNotNull(resource, "Fixture not found");

        Optional<Company> result =
                service.analyze(resource.toURI());

        assertTrue(result.isPresent());

        Company company = result.orElseThrow();

        assertEquals("Island Digital Studio",
                company.getName());

        assertEquals("Software Development",
                company.getIndustry());

        assertEquals("Ebene, Mauritius",
                company.getLocation());

        assertEquals("https://example.com",
                company.getWebsite());
    }

    @Test
    void shouldReturnEmptyWhenCompanyNameIsMissing()
            throws Exception {

        java.nio.file.Path file =
                java.nio.file.Files.createTempFile(
                        "company-without-name-", ".html"
                );

        try {
            java.nio.file.Files.writeString(
                    file,
                    "<p class='company-industry'>Technology</p>"
            );

            Optional<Company> result =
                    service.analyze(file.toUri());

            assertTrue(result.isEmpty());

        } finally {
            java.nio.file.Files.deleteIfExists(file);
        }
    }

    @Test
    void shouldPropagateFileReadingError() {

        URI missingFile = URI.create(
                "file:///nonexistent-studentcareer-service-12345.html"
        );

        assertThrows(
                IOException.class,
                () -> service.analyze(missingFile)
        );
    }
}
