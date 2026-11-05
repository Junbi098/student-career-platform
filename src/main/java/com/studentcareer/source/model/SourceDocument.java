package com.studentcareer.source.model;

import com.studentcareer.source.model.SourceReference;
import java.util.Objects;

/** Content and provenance are produced together by the reader. */
public record SourceDocument(String html, SourceReference source) {
    public SourceDocument {
        Objects.requireNonNull(html, "HTML is required");
        Objects.requireNonNull(source, "Source is required");
    }
}
