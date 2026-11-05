package com.studentcareer.source.reader;

import com.studentcareer.source.model.SourceDocument;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.model.SourceReference;
import com.studentcareer.source.reader.SourceReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;
import java.util.Set;

/** Reads only the bundled fictional demo resources, including inside an executable JAR. */
public class BundledFixtureSourceReader implements SourceReader {
    public static final Set<String> RESOURCES = Set.of(
            "/demo/company-complete.html", "/demo/company-duplicate.html",
            "/demo/company-second.html", "/demo/company-no-name.html");
    private final Clock clock;

    public BundledFixtureSourceReader(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public SourceDocument readSource(URI uri, SourceKind kind) throws IOException {
        if (uri == null || kind != SourceKind.FIXTURE || !"classpath".equals(uri.getScheme())
                || uri.getAuthority() != null || uri.getQuery() != null || uri.getFragment() != null
                || !RESOURCES.contains(uri.getPath())) {
            throw new IllegalArgumentException("Only bundled demo fixtures are supported");
        }
        try (var stream = getClass().getResourceAsStream(uri.getPath())) {
            if (stream == null) {
                throw new IOException("Bundled demo fixture is missing");
            }
            String html = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new SourceDocument(html, new SourceReference(uri,
                    "Fictional bundled fixture: " + uri.getPath(), clock.instant(), SourceKind.FIXTURE));
        }
    }
}
