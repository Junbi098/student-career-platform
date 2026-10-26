
package com.studentcareer.poc;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsoupTest {

    @Test
    void shouldExtractCompanyNameFromHtml() {

        // Arrange : page HTML fictive
        String html = """
                <html>
                    <body>
                        <h1 class="company-name">
                            Island Digital Studio
                        </h1>
                        <p>Software development company</p>
                    </body>
                </html>
                """;

        // Act : analyser le HTML
        CompanyNameExtractor extractor =
                new CompanyNameExtractor();

        Optional<String> companyName = extractor.extract(html);

        // Assert : vérifier le résultat
        assertEquals(
                Optional.of("Island Digital Studio"),
                companyName
        );
    }


    @Test
    void shouldReturnEmptyStringWhenCompanyNameIsMissing() {

        // Arrange
        String html = """
            <html>
                <body>
                    <p>Welcome to our website</p>
                </body>
            </html>
            """;

        CompanyNameExtractor extractor =
                new CompanyNameExtractor();

        // Act
        Optional<String> companyName = extractor.extract(html);

        // Assert
        assertEquals(
                Optional.empty(),
                companyName
        );
    }

}
