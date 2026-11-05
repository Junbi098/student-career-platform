package com.studentcareer.company.service;

import com.studentcareer.company.domain.Company;
import java.text.Normalizer;

/** Normalizes spacing, preserving spelling, case and website paths. */
public class CompanyNormalizer {
    public Company normalize(Company company) {
        return new Company(text(company.getName()), company.getWebsite(),
                text(company.getIndustry()), text(company.getLocation()));
    }

    private String text(String value) {
        if (value == null) {
            return null;
        }
        String cleaned = Normalizer.normalize(value, Normalizer.Form.NFC)
                .replaceAll("(?U)\\s+", " ").strip();
        return cleaned.isEmpty() ? null : cleaned;
    }
}
