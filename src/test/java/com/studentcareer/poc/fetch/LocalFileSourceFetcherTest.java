
package com.studentcareer.poc.fetch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileSourceFetcherTest {

    private final SourceFetcher fetcher =
            new LocalFileSourceFetcher();

    @Test
    void shouldReadLocalHtmlFixture() throws Exception {

        URL resource = getClass().getResource(
                "/fixtures/company-complete.html"
        );

        assertNotNull(resource, "HTML fixture not found");

        URI sourceUri = resource.toURI();

        String html = fetcher.fetch(sourceUri);

        assertTrue(html.contains("Island Digital Studio"));
        assertTrue(html.contains("Software Development"));
    }

    @Test
    void shouldRejectHttpUri() {
        URI sourceUri = URI.create("https://example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> fetcher.fetch(sourceUri)
        );
    }

    @Test
    void shouldRejectNullUri() {
        assertThrows(
                IllegalArgumentException.class,
                () -> fetcher.fetch(null)
        );
    }

    @Test
    void shouldReportMissingFile() {
        URI sourceUri = URI.create(
                "file:///nonexistent-studentcareer-test-12345.html"
        );

        assertThrows(
                IOException.class,
                () -> fetcher.fetch(sourceUri)
        );
    }
}
