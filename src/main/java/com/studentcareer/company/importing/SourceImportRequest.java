package com.studentcareer.company.importing;

import com.studentcareer.source.model.SourceKind;
import java.net.URI;
import java.util.Objects;

public record SourceImportRequest(URI uri, SourceKind kind) {
    public SourceImportRequest {
        Objects.requireNonNull(uri, "URI is required");
        Objects.requireNonNull(kind, "Source kind is required");
    }
}
