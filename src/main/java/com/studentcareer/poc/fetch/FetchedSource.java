package com.studentcareer.poc.fetch;

import com.studentcareer.poc.domain.model.SourceReference;
import java.util.Objects;

/** Content and provenance are produced together by the reader. */
public record FetchedSource(String html, SourceReference source) {
    public FetchedSource {
        Objects.requireNonNull(html, "HTML is required");
        Objects.requireNonNull(source, "Source is required");
    }
}
