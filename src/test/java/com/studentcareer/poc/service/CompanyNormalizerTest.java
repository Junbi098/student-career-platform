package com.studentcareer.poc.service;

import com.studentcareer.poc.domain.model.Company;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompanyNormalizerTest {
    private final CompanyNormalizer normalizer = new CompanyNormalizer();

    @Test
    void shouldNormalizeUnicodeAndWhitespaceWithoutChangingSpellingOrWebsite() {
        Company input = new Company("  Cafe\u0301\u00a0  Studio  ", "https://example.com/Team?x=A",
                " Software\t Development ", " Ebene\n Mauritius ");
        Company result = normalizer.normalize(input);
        assertEquals("Caf\u00e9 Studio", result.getName());
        assertEquals("Software Development", result.getIndustry());
        assertEquals("Ebene Mauritius", result.getLocation());
        assertEquals(input.getWebsite(), result.getWebsite());
    }

    @Test
    void shouldKeepUnknownFieldsUnknownAndNormalizeBlankFieldsToNull() {
        Company result = normalizer.normalize(new Company("Island", null, "\u00a0", null));
        assertNull(result.getWebsite());
        assertNull(result.getIndustry());
        assertNull(result.getLocation());
    }

    @Test
    void shouldNotModifyOriginalCompany() {
        Company input = new Company("Island   Studio", null, null, null);
        normalizer.normalize(input);
        assertEquals("Island   Studio", input.getName());
    }
}
