package com.studentcareer.opportunity.service;

import com.studentcareer.common.exception.InvalidRequestException;
import com.studentcareer.company.exception.CompanyNotFoundException;
import com.studentcareer.company.repository.CompanyRepository;
import com.studentcareer.opportunity.service.OpportunityContracts.*;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobPostingImportService {
    private final CompanyRepository companies;
    private final OpportunityService opportunities;
    private final Clock clock;
    private final JobPostingExtractor extractor = new JobPostingExtractor();

    public JobPostingImportService(CompanyRepository companies, OpportunityService opportunities, Clock clock) {
        this.companies = companies;
        this.opportunities = opportunities;
        this.clock = clock;
    }

    @Transactional
    public ImportReport ingest(HtmlImport request) {
        if (request == null || request.companyId() == null) { throw OpportunityRules.invalid("companyId is required"); }
        var company = companies.findById(request.companyId()).orElseThrow(CompanyNotFoundException::new);
        var extraction = extractor.extract(request, company.dossier().getCompany().getName());
        List<ImportItem> results = new ArrayList<>();
        int created = 0, updated = 0, unchanged = 0, rejected = 0;
        for (var candidate : extraction.candidates()) {
            if (candidate.error() != null) {
                results.add(new ImportItem(candidate.title(), null, "REJECTED", candidate.error())); rejected++; continue;
            }
            try {
                var draft = OpportunityRules.draft(candidate.input(), clock.instant());
                var result = opportunities.importDraft(draft);
                results.add(result);
                switch (result.outcome()) { case "CREATED" -> created++; case "UPDATED" -> updated++; default -> unchanged++; }
            } catch (InvalidRequestException exception) {
                results.add(new ImportItem(candidate.title(), null, "REJECTED", exception.getMessage())); rejected++;
            }
        }
        return new ImportReport(extraction.candidates().size(), created, updated, unchanged, rejected,
                List.copyOf(results), extraction.warnings());
    }
}
