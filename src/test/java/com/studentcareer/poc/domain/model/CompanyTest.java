
package com.studentcareer.poc.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompanyTest {

    @Test
    void shouldCreateCompanyWithValidName() {
        // Arrange & Act
        Company company = new Company(
                "Island Digital Studio",
                "https://example.com",
                "Software Development",
                "Ebene"
        );

        // Assert
        assertEquals("Island Digital Studio", company.getName());
        assertEquals("https://example.com", company.getWebsite());
        assertEquals("Software Development", company.getIndustry());
        assertEquals("Ebene", company.getLocation());
    }

    @Test
    void shouldRejectBlankCompanyName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Company("   ", null, null, null)
        );
    }

    @Test
    void shouldRejectNullCompanyName() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Company(null, null, null, null)
        );
    }

    @Test
    void shouldAcceptMissingOptionalInformation() {
        Company company = new Company(
                "Island Digital Studio",
                null,
                null,
                null
        );

        assertEquals("Island Digital Studio", company.getName());
        assertNull(company.getWebsite());
        assertNull(company.getIndustry());
        assertNull(company.getLocation());
    }
}
