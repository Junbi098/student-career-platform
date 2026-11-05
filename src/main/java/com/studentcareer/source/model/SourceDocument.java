package com.studentcareer.source.model;

import com.studentcareer.source.model.SourceReference;
import java.util.Objects;

/** Content and provenance are produced together by the reader. */
public record SourceDocument(String content, SourceReference source) {
    public SourceDocument {
        Objects.requireNonNull(content, "Content is required");
        Objects.requireNonNull(source, "Source is required");
    }
}
