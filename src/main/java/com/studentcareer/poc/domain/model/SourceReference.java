
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

        if (scheme == null
                || (!scheme.equalsIgnoreCase("http")
                && !scheme.equalsIgnoreCase("https"))
                || url.getHost() == null) {
            throw new IllegalArgumentException(
                    "Source URL must be HTTP or HTTPS with a host"
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
