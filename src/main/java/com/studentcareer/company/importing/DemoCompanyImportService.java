package com.studentcareer.company.importing;

import com.studentcareer.company.importing.BatchImportResult;
import com.studentcareer.company.importing.CompanyBatchImportService;
import com.studentcareer.company.importing.SourceImportRequest;
import com.studentcareer.company.parsing.CompanyHtmlParser;
import com.studentcareer.company.repository.InMemoryCompanyRepository;
import com.studentcareer.company.service.CompanyAnalysisService;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.reader.BundledFixtureSourceReader;
import java.net.URI;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DemoCompanyImportService {
    private final InMemoryCompanyRepository catalog;
    private final CompanyBatchImportService batch;

    public DemoCompanyImportService(InMemoryCompanyRepository catalog) {
        this.catalog = catalog;
        this.batch = new CompanyBatchImportService(new CompanyAnalysisService(
                new BundledFixtureSourceReader(Clock.systemUTC()), new CompanyHtmlParser()));
    }

    public BatchImportResult importDemo() {
        var requests = List.of("company-complete.html", "company-duplicate.html",
                        "company-second.html", "company-no-name.html").stream()
                .map(name -> new SourceImportRequest(URI.create("classpath:/demo/" + name), SourceKind.FIXTURE))
                .toList();
        BatchImportResult result = batch.analyze(requests);
        catalog.saveAll(result.items());
        return result;
    }
}
