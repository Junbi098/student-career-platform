
package com.studentcareer.poc.service;

import com.studentcareer.poc.CompanyHtmlParser;
import com.studentcareer.poc.domain.model.Company;
import com.studentcareer.poc.fetch.SourceFetcher;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;

public class CompanyIntelligenceService {

    private final SourceFetcher fetcher;
    private final CompanyHtmlParser parser;

    public CompanyIntelligenceService(
            SourceFetcher fetcher,
            CompanyHtmlParser parser
    ) {
        this.fetcher = Objects.requireNonNull(fetcher);
        this.parser = Objects.requireNonNull(parser);
    }

    public Optional<Company> analyze(URI sourceUri)
            throws IOException {

        String html = fetcher.fetch(sourceUri);

        return parser.parse(html);
    }
}
