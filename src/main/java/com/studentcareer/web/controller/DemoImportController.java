package com.studentcareer.web.controller;

import com.studentcareer.common.exception.InvalidRequestException;
import com.studentcareer.company.importing.DemoCompanyImportService;
import com.studentcareer.web.dto.CompanyResponses;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@RestController
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoImportController {
    private final DemoCompanyImportService service;

    public DemoImportController(DemoCompanyImportService service) {
        this.service = service;
    }

    @PostMapping("/api/imports/demo")
    public CompanyResponses.ImportReport importDemo(@RequestBody(required = false) String body) {
        if (body != null && !body.isBlank()) {
            throw new InvalidRequestException("Demo import does not accept a request body");
        }
        return CompanyResponses.ImportReport.from(service.importDemo());
    }
}
