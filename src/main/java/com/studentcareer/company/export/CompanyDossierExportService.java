package com.studentcareer.company.export;

import com.studentcareer.company.domain.CompanyDossier;
import com.studentcareer.company.export.CompanyDossierJsonWriter;
import com.studentcareer.company.service.CompanyAnalysisService;
import com.studentcareer.source.model.SourceKind;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class CompanyDossierExportService {
    private final CompanyAnalysisService service;
    private final CompanyDossierJsonWriter writer;

    public CompanyDossierExportService(CompanyAnalysisService service, CompanyDossierJsonWriter writer) {
        this.service = Objects.requireNonNull(service);
        this.writer = Objects.requireNonNull(writer);
    }

    public Optional<String> execute(URI sourceUri, SourceKind kind) throws IOException {
        Optional<CompanyDossier> dossier = service.analyzeDossier(sourceUri, kind);
        if (dossier.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(writer.write(dossier.orElseThrow()));
    }
}
