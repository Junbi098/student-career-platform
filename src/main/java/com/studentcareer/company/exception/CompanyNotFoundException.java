package com.studentcareer.company.exception;

import com.studentcareer.company.domain.Company;

public class CompanyNotFoundException extends RuntimeException {
    public CompanyNotFoundException() {
        super("Company dossier not found");
    }
}
