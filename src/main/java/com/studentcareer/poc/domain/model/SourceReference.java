
package com.studentcareer.poc.domain.model;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

public class SourceReference {

    private final URI url;
    private final String title;
    private final Instant accessedAt;

    public SourceReference(
            URI url,
            String title,
            Instant accessedAt
    ) {
        Objects.requireNonNull(url, "Source URL is required");

        String scheme = url.getScheme();

        boolean web = ("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme)) && url.getHost() != null;
        boolean local = "file".equalsIgnoreCase(scheme)
                && url.isAbsolute() && !url.isOpaque()
                && url.getPath() != null && url.getPath().startsWith("/")
                && url.getQuery() == null && url.getFragment() == null;
        if (!web && !local) {
            throw new IllegalArgumentException(
                    "Source must be an absolute local file URI or HTTP(S) URL with a host"
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Source title is required"
            );
        }

        this.url = url;
        this.title = title.trim();
        this.accessedAt = Objects.requireNonNull(
                accessedAt,
                "Access date is required"
        );
    }

    public URI getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public Instant getAccessedAt() {
        return accessedAt;
    }
}
