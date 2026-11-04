package com.studentcareer.poc.batch;

import com.studentcareer.poc.domain.model.CompanyDossier;
import java.net.URI;
import java.util.Objects;

/** A duplicate retains its own dossier and provenance; nothing is silently discarded. */
public record BatchItem(SourceRequest request, BatchStatus status,
                        CompanyDossier dossier, URI duplicateOf, String message) {
    public BatchItem {
        Objects.requireNonNull(request);
        Objects.requireNonNull(status);
        boolean hasCompany = status == BatchStatus.SUCCESS || status == BatchStatus.POSSIBLE_DUPLICATE;
        if (hasCompany != (dossier != null)) {
            throw new IllegalArgumentException("Only successful or duplicate results have a dossier");
        }
        if ((status == BatchStatus.POSSIBLE_DUPLICATE) != (duplicateOf != null)) {
            throw new IllegalArgumentException("Only duplicate results reference an earlier source");
        }
    }
}
