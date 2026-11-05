package com.studentcareer.company.repository;

import com.studentcareer.company.domain.CompanyCatalogEntry;
import com.studentcareer.company.importing.SourceImportResult;
import com.studentcareer.company.domain.CompanyDossierIdentity;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryCompanyRepository implements CompanyRepository {
    private final Map<UUID, CompanyCatalogEntry> entries = new LinkedHashMap<>();

    /** Each source retains its own dossier. Reimporting that source updates it in place. */
    public synchronized void saveAll(List<SourceImportResult> items) {
        for (SourceImportResult item : items) {
            if (item.dossier() == null) {
                continue;
            }
            var source = item.dossier().getSource();
            UUID id = CompanyDossierIdentity.from(source);
            entries.put(id, new CompanyCatalogEntry(id, item.dossier(), item.status(), item.duplicateOf()));
        }
    }

    public synchronized List<CompanyCatalogEntry> findAll() {
        return List.copyOf(entries.values());
    }

    public synchronized Optional<CompanyCatalogEntry> findById(UUID id) {
        return Optional.ofNullable(entries.get(id));
    }
}
