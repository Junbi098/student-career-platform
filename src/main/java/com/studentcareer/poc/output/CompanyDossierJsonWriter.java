
package com.studentcareer.poc.output;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.poc.domain.model.Company;
import com.studentcareer.poc.domain.model.CompanyDossier;
import com.studentcareer.poc.domain.model.SourceReference;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CompanyDossierJsonWriter {

    private final ObjectMapper mapper = new ObjectMapper();

    public String write(CompanyDossier dossier)
            throws JsonProcessingException {

        Objects.requireNonNull(dossier, "Dossier is required");

        Company company = dossier.getCompany();
        SourceReference source = dossier.getSource();

        Map<String, Object> companyData = new LinkedHashMap<>();
        companyData.put("name", company.getName());
        companyData.put("website", company.getWebsite());
        companyData.put("industry", company.getIndustry());
        companyData.put("location", company.getLocation());

        Map<String, Object> sourceData = new LinkedHashMap<>();
        sourceData.put("url", source.getUrl().toString());
        sourceData.put("title", source.getTitle());
        sourceData.put(
                "accessedAt",
                source.getAccessedAt().toString()
        );

        Map<String, Object> dossierData = new LinkedHashMap<>();
        dossierData.put("company", companyData);
        dossierData.put("source", sourceData);

        return mapper
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(dossierData);
    }
}
