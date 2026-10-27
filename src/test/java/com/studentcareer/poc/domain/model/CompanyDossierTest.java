
package com.studentcareer.poc.domain.model;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CompanyDossierTest {

    private Company createCompany() {
        return new Company(
                "Island Digital Studio",
                "https://example.com",
                "Software Development",
                "Ebene, Mauritius"
        );
    }

    private SourceReference createSource() {
        return new SourceReference(
                URI.create("https://example.com/about"),
                "About Island Digital Studio",
                Instant.parse("2026-10-27T12:00:00Z")
        );
    }

    @Test
    void shouldCreateValidDossier() {
        Company company = createCompany();
        SourceReference source = createSource();

        CompanyDossier dossier =
                new CompanyDossier(company, source);

        assertSame(company, dossier.getCompany());
        assertSame(source, dossier.getSource());
    }

    @Test
    void shouldRejectMissingCompany() {
        SourceReference source = createSource();

        assertThrows(
                NullPointerException.class,
                () -> new CompanyDossier(null, source)
        );
    }

    @Test
    void shouldRejectMissingSource() {
        Company company = createCompany();

        assertThrows(
                NullPointerException.class,
                () -> new CompanyDossier(company, null)
        );
    }
}
