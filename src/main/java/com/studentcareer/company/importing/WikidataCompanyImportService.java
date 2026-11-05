package com.studentcareer.company.importing;

import com.studentcareer.company.exception.CollectionRateLimitException;
import com.studentcareer.company.parsing.WikidataCompanyParser;
import com.studentcareer.company.repository.InMemoryCompanyRepository;
import com.studentcareer.company.service.CompanyAnalysisService;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.reader.SourceReader;
import com.studentcareer.source.reader.WikidataSourceReader;
import com.studentcareer.source.wikidata.WikidataSources;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WikidataCompanyImportService {
    private final InMemoryCompanyRepository repository;
    private final CompanyBatchImportService batch;
    private final Clock clock;
    private Instant lastAttempt;

    @Autowired
    public WikidataCompanyImportService(InMemoryCompanyRepository repository) {
        this(repository, new WikidataSourceReader(), Clock.systemUTC());
    }

    WikidataCompanyImportService(InMemoryCompanyRepository repository, SourceReader reader, Clock clock) {
        this.repository = repository;
        this.clock = clock;
        this.batch = new CompanyBatchImportService(new CompanyAnalysisService(reader, new WikidataCompanyParser()));
    }

    public synchronized BatchImportResult importCompanies() {
        Instant now = clock.instant();
        if (lastAttempt != null && now.isBefore(lastAttempt.plusSeconds(60))) {
            long remaining = Duration.between(now, lastAttempt.plusSeconds(60)).toSeconds();
            throw new CollectionRateLimitException(Math.max(1, remaining + 1));
        }
        // Apply the cooldown even after failure, to avoid hammering an unavailable source.
        lastAttempt = now;
        var requests = WikidataSources.SOURCES.stream()
                .map(source -> new SourceImportRequest(source.url(), SourceKind.WEB_OBSERVATION)).toList();
        BatchImportResult result = batch.analyze(requests);
        repository.saveAll(result.items());
        return result;
    }
}
