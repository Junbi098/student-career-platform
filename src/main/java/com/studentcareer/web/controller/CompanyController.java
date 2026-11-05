package com.studentcareer.web.controller;

import com.studentcareer.company.service.CompanyCatalogService;
import com.studentcareer.web.dto.CompanyResponses;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {
    private final CompanyCatalogService service;

    public CompanyController(CompanyCatalogService service) {
        this.service = service;
    }

    @GetMapping
    public CompanyResponses.CompanyPage search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "industry", required = false) String industry,
            @RequestParam(name = "location", required = false) String location,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return CompanyResponses.CompanyPage.from(service.search(query, industry, location, page, size));
    }

    @GetMapping("/{id}")
    public CompanyResponses.CompanyView find(@PathVariable("id") UUID id) {
        return CompanyResponses.CompanyView.from(service.find(id));
    }
}
