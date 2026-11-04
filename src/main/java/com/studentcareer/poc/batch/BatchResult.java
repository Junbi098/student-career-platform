package com.studentcareer.poc.batch;

import java.util.List;
import java.util.Objects;

public record BatchResult(List<BatchItem> items) {
    public BatchResult {
        items = List.copyOf(Objects.requireNonNull(items));
    }

    public long count(BatchStatus status) {
        Objects.requireNonNull(status);
        return items.stream().filter(item -> item.status() == status).count();
    }
}
