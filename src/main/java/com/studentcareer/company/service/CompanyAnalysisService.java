package com.studentcareer.company.service;

import com.studentcareer.company.domain.Company;
import com.studentcareer.company.domain.CompanyDossier;
import com.studentcareer.company.parsing.CompanyHtmlParser;
import com.studentcareer.source.model.SourceDocument;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.reader.SourceReader;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class CompanyAnalysisService {
    private final SourceReader fetcher;
    private final CompanyHtmlParser parser;
    private final CompanyNormalizer normalizer = new CompanyNormalizer();

    public CompanyAnalysisService(SourceReader fetcher, CompanyHtmlParser parser) {
        this.fetcher = Objects.requireNonNull(fetcher);
        this.parser = Objects.requireNonNull(parser);
    }

    public Optional<Company> analyze(URI sourceUri) throws IOException {
        return analyzeDossier(sourceUri, SourceKind.LOCAL_DOCUMENT).map(CompanyDossier::getCompany);
    }

    public Optional<CompanyDossier> analyzeDossier(URI sourceUri, SourceKind kind) throws IOException {
        SourceDocument fetched = fetcher.readSource(sourceUri, kind);
        return parser.parse(fetched.html()).map(normalizer::normalize)
                .map(company -> new CompanyDossier(company, fetched.source()));
    }
}
