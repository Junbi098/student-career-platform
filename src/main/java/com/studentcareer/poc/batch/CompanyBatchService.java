package com.studentcareer.poc.batch;

import com.studentcareer.poc.domain.model.Company;
import com.studentcareer.poc.domain.model.CompanyDossier;
import com.studentcareer.poc.domain.model.SourceKind;
import com.studentcareer.poc.service.CompanyIntelligenceService;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Sequential and deterministic: one result per input, in input order. */
public class CompanyBatchService {
    private final CompanyIntelligenceService service;

    public CompanyBatchService(CompanyIntelligenceService service) {
        this.service = Objects.requireNonNull(service);
    }

    public BatchResult analyze(List<SourceRequest> requests) {
        List<SourceRequest> inputs = List.copyOf(Objects.requireNonNull(requests));
        List<BatchItem> items = new ArrayList<>();
        Map<DuplicateKey, URI> firstSources = new HashMap<>();
        for (SourceRequest request : inputs) {
            // Restrict the catch to input/reading failures; programming errors must remain visible.
            Optional<CompanyDossier> result;
            try {
                result = service.analyzeDossier(request.uri(), request.kind());
            } catch (IOException exception) {
                items.add(new BatchItem(request, BatchStatus.READ_ERROR, null, null,
                        exception.getClass().getSimpleName() + ": " + exception.getMessage()));
                continue;
            } catch (IllegalArgumentException exception) {
                items.add(new BatchItem(request, BatchStatus.INVALID_SOURCE, null, null,
                        exception.getMessage()));
                continue;
            }
            if (result.isEmpty()) {
                items.add(new BatchItem(request, BatchStatus.NO_COMPANY, null, null,
                        "No company name found"));
                continue;
            }
            CompanyDossier dossier = result.orElseThrow();
            DuplicateKey key = duplicateKey(dossier.getCompany(), dossier.getSource().getKind());
            URI earlier = key == null ? null : firstSources.putIfAbsent(key, dossier.getSource().getUrl());
            items.add(new BatchItem(request,
                    earlier == null ? BatchStatus.SUCCESS : BatchStatus.POSSIBLE_DUPLICATE,
                    dossier, earlier, null));
        }
        return new BatchResult(items);
    }

    private DuplicateKey duplicateKey(Company company, SourceKind kind) {
        // A name alone is insufficient evidence. Never merge fixtures with other documents.
        if (company.getWebsite() == null) {
            return null;
        }
        URI website = URI.create(company.getWebsite());
        String scheme = website.getScheme().toLowerCase(Locale.ROOT);
        String host = website.getHost().toLowerCase(Locale.ROOT);
        int port = website.getPort();
        if (("https".equals(scheme) && port == 443) || ("http".equals(scheme) && port == 80)) {
            port = -1;
        }
        String path = website.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        // Keep path, query and user info: differences may identify distinct companies.
        return new DuplicateKey(company.getName().toLowerCase(Locale.ROOT), scheme, host,
                port, path, website.getRawQuery(), website.getRawUserInfo(), kind);
    }

    private record DuplicateKey(String name, String scheme, String host, int port,
                                String path, String query, String userInfo, SourceKind kind) { }
}
