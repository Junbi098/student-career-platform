package com.studentcareer.poc.pipeline;

import com.studentcareer.poc.domain.model.CompanyDossier;
import com.studentcareer.poc.domain.model.SourceKind;
import com.studentcareer.poc.output.CompanyDossierJsonWriter;
import com.studentcareer.poc.service.CompanyIntelligenceService;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class CompanyDossierPipeline {
    private final CompanyIntelligenceService service;
    private final CompanyDossierJsonWriter writer;

    public CompanyDossierPipeline(CompanyIntelligenceService service, CompanyDossierJsonWriter writer) {
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
