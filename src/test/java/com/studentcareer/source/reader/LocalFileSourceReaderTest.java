package com.studentcareer.source.reader;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileSourceReaderTest {

    private final SourceReader fetcher =
            new LocalFileSourceReader();

    @Test
    void shouldReadLocalHtmlFixture() throws Exception {

        URL resource = getClass().getResource(
                "/demo/company-complete.html"
        );

        assertNotNull(resource, "HTML fixture not found");

        URI sourceUri = resource.toURI();

        String html = fetcher.read(sourceUri);

        assertTrue(html.contains("Island Digital Studio"));
        assertTrue(html.contains("Software Development"));
    }

    @Test
    void shouldRejectHttpUri() {
        URI sourceUri = URI.create("https://example.com");

        assertThrows(
                IllegalArgumentException.class,
                () -> fetcher.read(sourceUri)
        );
    }

    @Test
    void shouldRejectNullUri() {
        assertThrows(
                IllegalArgumentException.class,
                () -> fetcher.read(null)
        );
    }

    @Test
    void shouldReportMissingFile() {
        URI sourceUri = URI.create(
                "file:///nonexistent-studentcareer-test-12345.html"
        );

        assertThrows(
                IOException.class,
                () -> fetcher.read(sourceUri)
        );
    }
}
