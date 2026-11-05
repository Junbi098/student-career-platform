package com.studentcareer.company.importing;

import com.studentcareer.company.domain.CompanyDossier;
import java.net.URI;
import java.util.Objects;

/** A duplicate retains its own dossier and provenance; nothing is silently discarded. */
public record SourceImportResult(SourceImportRequest request, ImportStatus status,
                        CompanyDossier dossier, URI duplicateOf, String message) {
    public SourceImportResult {
        Objects.requireNonNull(request);
        Objects.requireNonNull(status);
        boolean hasCompany = status == ImportStatus.SUCCESS || status == ImportStatus.POSSIBLE_DUPLICATE;
        if (hasCompany != (dossier != null)) {
            throw new IllegalArgumentException("Only successful or duplicate results have a dossier");
        }
        if ((status == ImportStatus.POSSIBLE_DUPLICATE) != (duplicateOf != null)) {
            throw new IllegalArgumentException("Only duplicate results reference an earlier source");
        }
    }
}
