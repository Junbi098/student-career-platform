package com.studentcareer.poc.batch;

import com.studentcareer.poc.domain.model.SourceKind;
import java.net.URI;
import java.util.Objects;

public record SourceRequest(URI uri, SourceKind kind) {
    public SourceRequest {
        Objects.requireNonNull(uri, "URI is required");
        Objects.requireNonNull(kind, "Source kind is required");
    }
}
