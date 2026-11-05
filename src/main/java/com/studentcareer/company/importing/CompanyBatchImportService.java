package com.studentcareer.company.importing;

import com.studentcareer.company.domain.Company;
import com.studentcareer.company.domain.CompanyDossier;
import com.studentcareer.company.service.CompanyAnalysisService;
import com.studentcareer.source.model.SourceKind;
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
public class CompanyBatchImportService {
    private final CompanyAnalysisService service;

    public CompanyBatchImportService(CompanyAnalysisService service) {
        this.service = Objects.requireNonNull(service);
    }

    public BatchImportResult analyze(List<SourceImportRequest> requests) {
        List<SourceImportRequest> inputs = List.copyOf(Objects.requireNonNull(requests));
        List<SourceImportResult> items = new ArrayList<>();
        Map<DuplicateKey, URI> firstSources = new HashMap<>();
        for (SourceImportRequest request : inputs) {
            // Restrict the catch to input/reading failures; programming errors must remain visible.
            Optional<CompanyDossier> result;
            try {
                result = service.analyzeDossier(request.uri(), request.kind());
            } catch (IOException exception) {
                items.add(new SourceImportResult(request, ImportStatus.READ_ERROR, null, null,
                        exception.getClass().getSimpleName() + ": " + exception.getMessage()));
                continue;
            } catch (IllegalArgumentException exception) {
                items.add(new SourceImportResult(request, ImportStatus.INVALID_SOURCE, null, null,
                        exception.getMessage()));
                continue;
            }
            if (result.isEmpty()) {
                items.add(new SourceImportResult(request, ImportStatus.NO_COMPANY, null, null,
                        "No eligible company found in source"));
                continue;
            }
            CompanyDossier dossier = result.orElseThrow();
            DuplicateKey key = duplicateKey(dossier.getCompany(), dossier.getSource().getKind());
            URI earlier = key == null ? null : firstSources.putIfAbsent(key, dossier.getSource().getUrl());
            items.add(new SourceImportResult(request,
                    earlier == null ? ImportStatus.SUCCESS : ImportStatus.POSSIBLE_DUPLICATE,
                    dossier, earlier, null));
        }
        return new BatchImportResult(items);
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
