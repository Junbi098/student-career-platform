package com.studentcareer.company.domain;

import com.studentcareer.source.model.SourceReference;
import java.util.Objects;

public class CompanyDossier {

    private final Company company;
    private final SourceReference source;

    public CompanyDossier(
            Company company,
            SourceReference source
    ) {
        this.company = Objects.requireNonNull(
                company,
                "Company is required"
        );

        this.source = Objects.requireNonNull(
                source,
                "Source reference is required"
        );
    }

    public Company getCompany() {
        return company;
    }

    public SourceReference getSource() {
        return source;
    }
}
