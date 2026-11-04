package com.studentcareer.poc.fetch;

import com.studentcareer.poc.domain.model.SourceKind;
import com.studentcareer.poc.domain.model.SourceReference;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class LocalFileSourceFetcher implements SourceFetcher {
    private final Clock clock;

    public LocalFileSourceFetcher() {
        this(Clock.systemUTC());
    }

    public LocalFileSourceFetcher(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock is required");
    }

    @Override
    public FetchedSource fetchSource(URI sourceUri, SourceKind kind) throws IOException {
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
        return new FetchedSource(html,
                new SourceReference(path.toUri(), title, accessedAt, kind));
    }
}
