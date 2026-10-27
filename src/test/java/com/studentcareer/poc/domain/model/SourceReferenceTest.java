
package com.studentcareer.poc.domain.model;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SourceReferenceTest {

    private final Instant accessedAt =
            Instant.parse("2026-10-27T12:00:00Z");

    @Test
    void shouldCreateValidSourceReference() {
        SourceReference source = new SourceReference(
                URI.create("https://example.com/about"),
                "About Island Digital Studio",
                accessedAt
        );

        assertEquals(
                URI.create("https://example.com/about"),
                source.getUrl()
        );
        assertEquals(
                "About Island Digital Studio",
                source.getTitle()
        );
        assertEquals(accessedAt, source.getAccessedAt());
    }

    @Test
    void shouldRejectBlankTitle() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SourceReference(
                        URI.create("https://example.com"),
                        "   ",
                        accessedAt
                )
        );
    }

    @Test
    void shouldRejectUnsupportedUrlScheme() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SourceReference(
                        URI.create("file:///example.html"),
                        "Local file",
                        accessedAt
                )
        );
    }

    @Test
    void shouldRejectMissingAccessDate() {
        assertThrows(
                NullPointerException.class,
                () -> new SourceReference(
                        URI.create("https://example.com"),
                        "Example",
                        null
                )
        );
    }

    @Test
    void shouldRejectMissingUrl() {
        assertThrows(
                NullPointerException.class,
                () -> new SourceReference(
                        null,
                        "Example",
                        accessedAt
                )
        );
    }
}
