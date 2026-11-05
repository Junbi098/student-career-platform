package com.studentcareer.web.controller;

import com.studentcareer.common.exception.InvalidRequestException;
import com.studentcareer.company.importing.WikidataCompanyImportService;
import com.studentcareer.source.wikidata.WikidataSources;
import com.studentcareer.web.dto.CompanyResponses;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WikidataImportController {
    private final WikidataCompanyImportService service;

    public WikidataImportController(WikidataCompanyImportService service) {
        this.service = service;
    }

    @GetMapping("/api/sources")
    public SourceCatalog sources() {
        return new SourceCatalog("CURATED_SEEDS", "UNVERIFIED", WikidataSources.SOURCES);
    }

    @PostMapping("/api/imports/wikidata")
    public CompanyResponses.ImportReport importCompanies(@RequestBody(required = false) String body) {
        if (body != null && !body.isBlank()) {
            throw new InvalidRequestException("Wikidata import does not accept a request body or arbitrary URL");
        }
        return CompanyResponses.ImportReport.from(service.importCompanies(), "WIKIDATA_OPEN_DATA");
    }

    public record SourceCatalog(String discoveryMode, String verificationStatus, List<WikidataSources.Source> sources) { }
}
