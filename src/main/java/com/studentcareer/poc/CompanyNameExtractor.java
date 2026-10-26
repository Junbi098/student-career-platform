
package com.studentcareer.poc;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.Optional;

public class CompanyNameExtractor {

    public Optional<String> extract(String html) {
        Document document = Jsoup.parse(html);

        String companyName = document
                .select(".company-name")
                .text()
                .trim();

        if (companyName.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(companyName);
    }
}
