package com.studentcareer.source.model;

import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;

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
                        URI.create("ftp://example.com/example.html"),
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
    @Test
    void shouldAcceptLocalSource() {
        URI uri = URI.create("file:///C:/fixtures/company.html");
        SourceReference source = new SourceReference(uri, "Fictional local fixture", accessedAt);
        assertEquals(uri, source.getUrl());
    }

    @Test
    void shouldRejectWebUrlWithoutHost() {
        assertThrows(IllegalArgumentException.class, () -> new SourceReference(
                URI.create("https:/missing-host"), "Invalid source", accessedAt));
    }

    @Test
    void shouldAcceptClasspathOnlyWhenExplicitlyFictional() {
        URI uri = URI.create("classpath:/demo/company-complete.html");
        var reference = new SourceReference(uri, "Fictional fixture", accessedAt, SourceKind.FIXTURE);
        assertEquals(uri, reference.getUrl());
        assertThrows(IllegalArgumentException.class,
                () -> new SourceReference(uri, "Local document", accessedAt, SourceKind.LOCAL_DOCUMENT));
        assertThrows(IllegalArgumentException.class,
                () -> new SourceReference(uri, "Unclassified", accessedAt));
    }

    @Test
    void shouldRejectClasspathReferenceWithAuthorityOrQuery() {
        assertThrows(IllegalArgumentException.class, () -> new SourceReference(
                URI.create("classpath://host/demo/file.html"), "Fixture", accessedAt, SourceKind.FIXTURE));
        assertThrows(IllegalArgumentException.class, () -> new SourceReference(
                URI.create("classpath:/demo/file.html?x=1"), "Fixture", accessedAt, SourceKind.FIXTURE));
    }
    @Test
    void shouldDistinguishFictionalAndUnverifiedSources() {
        var fixture = new SourceReference(URI.create("classpath:/demo/company.html"), "Fixture", accessedAt, SourceKind.FIXTURE);
        assertEquals(SourceVerificationStatus.FICTIONAL, fixture.getVerificationStatus());
        var web = new SourceReference(URI.create("https://example.com/company.json"), "Open data", accessedAt,
                SourceKind.WEB_OBSERVATION, Instant.EPOCH, "CC0-1.0");
        assertEquals(SourceVerificationStatus.UNVERIFIED, web.getVerificationStatus());
        assertEquals(Instant.EPOCH, web.getSourceUpdatedAt());
        assertThrows(IllegalArgumentException.class, () -> new SourceReference(
                URI.create("file:///local.html"), "Not web", accessedAt, SourceKind.WEB_OBSERVATION));
    }
}
