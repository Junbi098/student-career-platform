package com.studentcareer.company.persistence;

import com.studentcareer.company.domain.Company;
import com.studentcareer.company.domain.CompanyCatalogEntry;
import com.studentcareer.company.domain.CompanyDossier;
import com.studentcareer.company.domain.CompanyDossierIdentity;
import com.studentcareer.company.importing.ImportStatus;
import com.studentcareer.company.importing.SourceImportResult;
import com.studentcareer.company.repository.CompanyRepository;
import com.studentcareer.source.model.SourceReference;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
public class JpaCompanyRepository implements CompanyRepository {
    private final CompanyDossierJpaRepository dossiers;
    private final TransactionTemplate transactions;

    public JpaCompanyRepository(CompanyDossierJpaRepository dossiers, PlatformTransactionManager manager) {
        this.dossiers = dossiers;
        this.transactions = new TransactionTemplate(manager);
    }

    // The lock encloses the transaction's commit, serializing imports within this application instance.
    @Override
    public synchronized void saveAll(List<SourceImportResult> items) {
        transactions.executeWithoutResult(transaction -> {
            for (SourceImportResult item : List.copyOf(items)) {
                if (item.dossier() == null) {
                    continue;
                }
                var company = item.dossier().getCompany();
                var source = item.dossier().getSource();
                UUID id = CompanyDossierIdentity.from(source);
                CompanyDossierEntity entity = dossiers.findById(id).orElseGet(CompanyDossierEntity::new);
                entity.id = id;
                entity.name = company.getName();
                entity.website = company.getWebsite();
                entity.industry = company.getIndustry();
                entity.location = company.getLocation();
                entity.importStatus = item.status().name();
                entity.duplicateOf = item.duplicateOf() == null ? null : item.duplicateOf().toString();
                if (entity.source == null) {
                    entity.source = new SourceReferenceEntity();
                }
                entity.source.updateFrom(id, source);
                dossiers.save(entity);
            }
            dossiers.flush();
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompanyCatalogEntry> findAll() {
        return dossiers.findAllByOrderByImportOrderAsc().stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CompanyCatalogEntry> findById(UUID id) {
        return dossiers.findById(id).map(this::toDomain);
    }

    private CompanyCatalogEntry toDomain(CompanyDossierEntity entity) {
        SourceReference source = entity.source.toDomain();
        Company company = new Company(entity.name, entity.website, entity.industry, entity.location);
        return new CompanyCatalogEntry(entity.id, new CompanyDossier(company, source),
                ImportStatus.valueOf(entity.importStatus),
                entity.duplicateOf == null ? null : URI.create(entity.duplicateOf));
    }
}
