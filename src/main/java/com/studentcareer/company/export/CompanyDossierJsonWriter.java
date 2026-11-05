package com.studentcareer.company.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.company.domain.Company;
import com.studentcareer.company.domain.CompanyDossier;
import com.studentcareer.source.model.SourceReference;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class CompanyDossierJsonWriter {

    private final ObjectMapper mapper = new ObjectMapper();

    public String write(CompanyDossier dossier)
            throws JsonProcessingException {

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(toData(dossier));
    }

    Map<String, Object> toData(CompanyDossier dossier) {
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
        sourceData.put("kind", source.getKind().name());
        sourceData.put("verifiedAt", null);
        sourceData.put("verificationStatus", source.getVerificationStatus().name());
        sourceData.put("license", source.getLicense());
        sourceData.put("sourceUpdatedAt", source.getSourceUpdatedAt() == null ? null : source.getSourceUpdatedAt().toString());
        sourceData.put(
                "accessedAt",
                source.getAccessedAt().toString()
        );

        Map<String, Object> dossierData = new LinkedHashMap<>();
        dossierData.put("company", companyData);
        dossierData.put("source", sourceData);

        return dossierData;
    }
}
