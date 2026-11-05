package com.studentcareer.source.model;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

public class SourceReference {

    private final URI url;
    private final String title;
    private final Instant accessedAt;
    private final SourceKind kind;
    private final Instant sourceUpdatedAt;
    private final String license;

    public SourceReference(
            URI url,
            String title,
            Instant accessedAt
    ) {
        this(url, title, accessedAt, SourceKind.UNCLASSIFIED);
    }

    public SourceReference(URI url, String title, Instant accessedAt, SourceKind kind) {
        this(url, title, accessedAt, kind, null, null);
    }

    public SourceReference(URI url, String title, Instant accessedAt, SourceKind kind,
                           Instant sourceUpdatedAt, String license) {
        Objects.requireNonNull(url, "Source URL is required");

        String scheme = url.getScheme();

        boolean web = ("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme)) && url.getHost() != null;
        boolean local = "file".equalsIgnoreCase(scheme)
                && url.isAbsolute() && !url.isOpaque()
                && url.getPath() != null && url.getPath().startsWith("/")
                && url.getQuery() == null && url.getFragment() == null;
        boolean fixture = kind == SourceKind.FIXTURE
                && "classpath".equalsIgnoreCase(scheme) && !url.isOpaque()
                && url.getAuthority() == null && url.getPath() != null
                && url.getPath().startsWith("/") && url.getQuery() == null && url.getFragment() == null;
        if (!web && !local && !fixture) {
            throw new IllegalArgumentException(
                    "Source must be a local file, HTTP(S) URL with a host, or classpath fixture"
            );
        }

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException(
                    "Source title is required"
            );
        }

        this.kind = Objects.requireNonNull(kind, "Source kind is required");
        if (kind == SourceKind.WEB_OBSERVATION && !web) {
            throw new IllegalArgumentException("A web observation requires a web URL");
        }
        this.sourceUpdatedAt = sourceUpdatedAt;
        this.license = license;
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
    public SourceKind getKind() {
        return kind;
    }
    public Instant getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public String getLicense() {
        return license;
    }

    public SourceVerificationStatus getVerificationStatus() {
        return kind == SourceKind.FIXTURE ? SourceVerificationStatus.FICTIONAL : SourceVerificationStatus.UNVERIFIED;
    }
}
