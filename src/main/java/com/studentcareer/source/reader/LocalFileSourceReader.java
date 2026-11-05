package com.studentcareer.source.reader;

import com.studentcareer.source.model.SourceDocument;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.model.SourceReference;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class LocalFileSourceReader implements SourceReader {
    private final Clock clock;

    public LocalFileSourceReader() {
        this(Clock.systemUTC());
    }

    public LocalFileSourceReader(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    @Override
    public SourceDocument readSource(URI sourceUri, SourceKind kind) throws IOException {
        if (sourceUri == null || !"file".equalsIgnoreCase(sourceUri.getScheme())) {
            throw new IllegalArgumentException("Only absolute local file URIs are supported");
        }
        if (kind != SourceKind.FIXTURE && kind != SourceKind.LOCAL_DOCUMENT) {
            throw new IllegalArgumentException("A local source must be FIXTURE or LOCAL_DOCUMENT");
        }
        Path path = Path.of(sourceUri).toRealPath();
        String html = Files.readString(path, StandardCharsets.UTF_8);
        Instant accessedAt = clock.instant();
        String title = (kind == SourceKind.FIXTURE ? "Fictional local fixture: " : "Local document: ")
                + path.getFileName();
        return new SourceDocument(html,
                new SourceReference(path.toUri(), title, accessedAt, kind));
    }
}
