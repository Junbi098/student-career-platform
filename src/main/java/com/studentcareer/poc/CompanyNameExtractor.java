package com.studentcareer.poc;

import java.util.Optional;

public class CompanyNameExtractor {

    public Optional<String> extract(String html) {
        return new CompanyHtmlParser().parse(html)
                .map(company -> company.getName());
    }
}
