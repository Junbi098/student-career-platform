package com.studentcareer.poc.service;

import com.studentcareer.poc.CompanyHtmlParser;
import com.studentcareer.poc.domain.model.Company;
import com.studentcareer.poc.domain.model.CompanyDossier;
import com.studentcareer.poc.domain.model.SourceKind;
import com.studentcareer.poc.fetch.FetchedSource;
import com.studentcareer.poc.fetch.SourceFetcher;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class CompanyIntelligenceService {
    private final SourceFetcher fetcher;
    private final CompanyHtmlParser parser;
    private final CompanyNormalizer normalizer = new CompanyNormalizer();

    public CompanyIntelligenceService(SourceFetcher fetcher, CompanyHtmlParser parser) {
        this.fetcher = Objects.requireNonNull(fetcher);
        this.parser = Objects.requireNonNull(parser);
    }

    public Optional<Company> analyze(URI sourceUri) throws IOException {
        return analyzeDossier(sourceUri, SourceKind.LOCAL_DOCUMENT).map(CompanyDossier::getCompany);
    }

    public Optional<CompanyDossier> analyzeDossier(URI sourceUri, SourceKind kind) throws IOException {
        FetchedSource fetched = fetcher.fetchSource(sourceUri, kind);
        return parser.parse(fetched.html()).map(normalizer::normalize)
                .map(company -> new CompanyDossier(company, fetched.source()));
    }
}
