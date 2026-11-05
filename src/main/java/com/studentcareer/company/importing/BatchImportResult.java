package com.studentcareer.company.importing;

import java.util.List;
import java.util.Objects;

public record BatchImportResult(List<SourceImportResult> items) {
    public BatchImportResult {
        items = List.copyOf(Objects.requireNonNull(items));
    }

    public long count(ImportStatus status) {
        Objects.requireNonNull(status);
        return items.stream().filter(item -> item.status() == status).count();
    }
}
