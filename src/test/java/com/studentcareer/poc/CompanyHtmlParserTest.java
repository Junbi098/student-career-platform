
package com.studentcareer.poc;

import com.studentcareer.poc.domain.model.Company;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CompanyHtmlParserTest {

    private final CompanyHtmlParser parser =
            new CompanyHtmlParser();

    @Test
    void shouldExtractCompanyFromHtmlFixture()
            throws Exception {

        String html;

        try (InputStream stream = getClass()
                .getResourceAsStream(
                        "/fixtures/company-complete.html"
                )) {

            assertNotNull(stream, "HTML fixture is missing");

            html = new String(
                    stream.readAllBytes(),
                    StandardCharsets.UTF_8
            );
        }

        Optional<Company> result = parser.parse(html);

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
    void shouldAcceptMissingOptionalFields() {
        String html = """
                <h1 class="company-name">
                    Island Digital Studio
                </h1>
                """;

        Company company = parser.parse(html).orElseThrow();

        assertEquals("Island Digital Studio",
                company.getName());

        assertNull(company.getIndustry());
        assertNull(company.getLocation());
        assertNull(company.getWebsite());
    }

    @Test
    void shouldReturnEmptyWhenCompanyNameIsMissing() {
        String html = """
                <p class="company-industry">
                    Software Development
                </p>
                """;

        assertTrue(parser.parse(html).isEmpty());
    }

    @Test
    void shouldReturnEmptyForBlankCompanyName() {
        String html = """
                <h1 class="company-name">   </h1>
                """;

        assertTrue(parser.parse(html).isEmpty());
    }

    @Test
    void shouldReturnEmptyForNullHtml() {
        assertTrue(parser.parse(null).isEmpty());
    }
}
