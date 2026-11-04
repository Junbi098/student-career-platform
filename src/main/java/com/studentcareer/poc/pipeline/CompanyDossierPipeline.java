
package com.studentcareer.poc.pipeline;

import com.studentcareer.poc.domain.model.Company;
import com.studentcareer.poc.domain.model.CompanyDossier;
import com.studentcareer.poc.domain.model.SourceReference;
import com.studentcareer.poc.output.CompanyDossierJsonWriter;
import com.studentcareer.poc.service.CompanyIntelligenceService;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class CompanyDossierPipeline {

    private final CompanyIntelligenceService service;
    private final CompanyDossierJsonWriter writer;

    public CompanyDossierPipeline(
            CompanyIntelligenceService service,
            CompanyDossierJsonWriter writer
    ) {
        this.service = Objects.requireNonNull(service);
        this.writer = Objects.requireNonNull(writer);
    }

    public Optional<String> execute(
            URI localFileUri,
            SourceReference source
    ) throws IOException {

        Objects.requireNonNull(source, "Source is required");

        if (localFileUri == null || !localFileUri.normalize().equals(source.getUrl().normalize())) {
            throw new IllegalArgumentException("Source reference must match the URI being read");
        }

        Optional<Company> company =
                service.analyze(localFileUri);

        if (company.isEmpty()) {
            return Optional.empty();
        }

        CompanyDossier dossier =
                new CompanyDossier(
                        company.orElseThrow(),
                        source
                );

        return Optional.of(writer.write(dossier));
    }
}
