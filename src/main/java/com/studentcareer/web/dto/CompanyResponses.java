package com.studentcareer.web.dto;

import com.studentcareer.company.domain.Company;
import com.studentcareer.company.domain.CompanyCatalogEntry;
import com.studentcareer.company.domain.CompanyDossier;
import com.studentcareer.company.importing.BatchImportResult;
import com.studentcareer.company.importing.ImportStatus;
import com.studentcareer.company.service.CompanyCatalogService.CatalogPage;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.model.SourceReference;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** HTTP representations are separate from the POC's domain objects. */
public final class CompanyResponses {
    private CompanyResponses() { }

    public record CompanyData(String name, String website, String industry, String location) {
        public static CompanyData from(Company company) {
            return new CompanyData(company.getName(), company.getWebsite(),
                    company.getIndustry(), company.getLocation());
        }
    }

    public record SourceData(URI url, String title, SourceKind kind, Instant accessedAt) {
        public static SourceData from(SourceReference source) {
            return new SourceData(source.getUrl(), source.getTitle(), source.getKind(), source.getAccessedAt());
        }
    }

    public record DossierData(CompanyData company, SourceData source) {
        public static DossierData from(CompanyDossier dossier) {
            return new DossierData(CompanyData.from(dossier.getCompany()), SourceData.from(dossier.getSource()));
        }
    }

    public record CompanyView(UUID id, CompanyData company, SourceData source,
                              ImportStatus importStatus, URI duplicateOf) {
        public static CompanyView from(CompanyCatalogEntry entry) {
            return new CompanyView(entry.id(), CompanyData.from(entry.dossier().getCompany()),
                    SourceData.from(entry.dossier().getSource()), entry.importStatus(), entry.duplicateOf());
        }
    }

    public record CompanyPage(List<CompanyView> items, int page, int size,
                              long totalElements, int totalPages) {
        public static CompanyPage from(CatalogPage page) {
            return new CompanyPage(page.items().stream().map(CompanyView::from).toList(),
                    page.page(), page.size(), page.totalElements(), page.totalPages());
        }
    }

    public record ImportItem(URI requestedUri, ImportStatus status, DossierData dossier,
                             URI duplicateOf, String message) { }

    public record ImportReport(String mode, int total, Map<String, Long> summary, List<ImportItem> results) {
        public static ImportReport from(BatchImportResult batch) {
            Map<String, Long> counts = new LinkedHashMap<>();
            for (ImportStatus status : ImportStatus.values()) {
                counts.put(status.name(), batch.count(status));
            }
            return new ImportReport("FICTIONAL_DEMO", batch.items().size(), counts,
                    batch.items().stream().map(item -> new ImportItem(item.request().uri(), item.status(),
                            item.dossier() == null ? null : DossierData.from(item.dossier()),
                            item.duplicateOf(), item.message())).toList());
        }
    }
}
