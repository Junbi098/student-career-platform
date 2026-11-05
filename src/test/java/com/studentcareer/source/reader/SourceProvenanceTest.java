package com.studentcareer.source.reader;

import com.studentcareer.source.model.SourceKind;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class SourceProvenanceTest {
    @TempDir Path directory;
    private final Instant instant = Instant.parse("2026-11-04T12:00:00Z");
    private final LocalFileSourceReader fetcher = new LocalFileSourceReader(
            Clock.fixed(instant, ZoneOffset.UTC));

    @Test
    void shouldProduceContentRealUriTimeAndFixtureKindTogether() throws Exception {
        Path file = directory.resolve("company with spaces.html");
        Files.writeString(file, "<h1>Fictional</h1>");
        var result = fetcher.readSource(directory.resolve(".").resolve(file.getFileName()).toUri(),
                SourceKind.FIXTURE);
        assertEquals("<h1>Fictional</h1>", result.content());
        assertEquals(file.toRealPath().toUri(), result.source().getUrl());
        assertEquals(instant, result.source().getAccessedAt());
        assertEquals(SourceKind.FIXTURE, result.source().getKind());
        assertTrue(result.source().getTitle().startsWith("Fictional local fixture:"));
    }

    @Test
    void shouldDistinguishLocalDocumentFromFixtureWithoutClaimingWebVerification() throws Exception {
        Path file = Files.writeString(directory.resolve("saved.html"), "saved content");
        var source = fetcher.readSource(file.toUri(), SourceKind.LOCAL_DOCUMENT).source();
        assertEquals(SourceKind.LOCAL_DOCUMENT, source.getKind());
        assertEquals("file", source.getUrl().getScheme());
        assertEquals("Local document: saved.html", source.getTitle());
    }

    @Test
    void shouldRefuseUnclassifiedAndNullKinds() throws Exception {
        Path file = Files.writeString(directory.resolve("saved.html"), "saved content");
        assertThrows(IllegalArgumentException.class,
                () -> fetcher.readSource(file.toUri(), SourceKind.UNCLASSIFIED));
        assertThrows(IllegalArgumentException.class, () -> fetcher.readSource(file.toUri(), null));
    }

    @Test
    void shouldRejectFileUriWithQuery() {
        assertThrows(IllegalArgumentException.class, () -> fetcher.readSource(
                directory.resolve("input.html").toUri().resolve("input.html?query=1"), SourceKind.FIXTURE));
    }
}
