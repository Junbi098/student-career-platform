package com.studentcareer.company.repository;

import com.studentcareer.company.domain.CompanyCatalogEntry;
import com.studentcareer.company.importing.SourceImportResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Storage contract, independent of JPA and of the HTTP API. */
public interface CompanyRepository {
    void saveAll(List<SourceImportResult> items);
    List<CompanyCatalogEntry> findAll();
    Optional<CompanyCatalogEntry> findById(UUID id);
}
