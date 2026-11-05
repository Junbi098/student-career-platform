package com.studentcareer.source.reader;

import com.studentcareer.source.model.SourceKind;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BundledFixtureSourceReaderTest {
    private final Instant instant = Instant.parse("2026-11-04T12:00:00Z");
    private final BundledFixtureSourceReader fetcher = new BundledFixtureSourceReader(
            Clock.fixed(instant, ZoneOffset.UTC));

    @Test
    void shouldReadBundledFixtureAndCreateHonestProvenance() throws Exception {
        URI uri = URI.create("classpath:/demo/company-complete.html");
        var result = fetcher.readSource(uri, SourceKind.FIXTURE);
        assertTrue(result.content().contains("Island Digital Studio"));
        assertEquals(uri, result.source().getUrl());
        assertEquals(instant, result.source().getAccessedAt());
        assertEquals(SourceKind.FIXTURE, result.source().getKind());
    }

    @Test
    void shouldNotExposeOtherResourcesOrResolveArbitraryUris() {
        for (String value : new String[]{"classpath:/application.properties", "classpath:/demo/../application.properties",
                "classpath:/demo/company-complete.html?x=1", "file:///private.html", "https://example.com"}) {
            assertThrows(IllegalArgumentException.class,
                    () -> fetcher.readSource(URI.create(value), SourceKind.FIXTURE));
        }
    }

    @Test
    void shouldNotMislabelBundledFictionAsLocalDocument() {
        assertThrows(IllegalArgumentException.class, () -> fetcher.readSource(
                URI.create("classpath:/demo/company-complete.html"), SourceKind.LOCAL_DOCUMENT));
    }
}
