
package com.studentcareer.poc.output;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.poc.domain.model.Company;
import com.studentcareer.poc.domain.model.CompanyDossier;
import com.studentcareer.poc.domain.model.SourceReference;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CompanyDossierJsonWriterTest {

    private final CompanyDossierJsonWriter writer =
            new CompanyDossierJsonWriter();

    private final ObjectMapper mapper = new ObjectMapper();

    private SourceReference createSource() {
        return new SourceReference(
                URI.create("https://example.com/about"),
                "Fictional company profile",
                Instant.parse("2026-10-27T12:00:00Z")
        );
    }

    @Test
    void shouldGenerateCompleteJson() throws Exception {

        Company company = new Company(
                "Island Digital Studio",
                "https://example.com",
                "Software Development",
                "Ebene, Mauritius"
        );

        CompanyDossier dossier =
                new CompanyDossier(company, createSource());

        String json = writer.write(dossier);

        JsonNode root = mapper.readTree(json);

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
                "https://example.com/about",
                root.path("source").path("url").asText()
        );

        assertEquals(
                "Fictional company profile",
                root.path("source").path("title").asText()
        );

        assertEquals(
                "2026-10-27T12:00:00Z",
                root.path("source").path("accessedAt").asText()
        );
    }

    @Test
    void shouldPreserveMissingOptionalFields() throws Exception {

        Company company = new Company(
                "Island Digital Studio",
                null,
                null,
                null
        );

        CompanyDossier dossier =
                new CompanyDossier(company, createSource());

        String json = writer.write(dossier);

        JsonNode companyNode =
                mapper.readTree(json).path("company");

        assertTrue(companyNode.path("website").isNull());
        assertTrue(companyNode.path("industry").isNull());
        assertTrue(companyNode.path("location").isNull());
    }

    @Test
    void shouldRejectNullDossier() {
        assertThrows(
                NullPointerException.class,
                () -> writer.write(null)
        );
    }
}
