
package com.studentcareer.poc;

import com.studentcareer.poc.domain.model.Company;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Optional;
import java.net.URI;

public class CompanyHtmlParser {

    public Optional<Company> parse(String html) {
        if (html == null) {
            return Optional.empty();
        }

        Document document = Jsoup.parse(html);

        String name = extractText(document, ".company-name");

        if (name == null) {
            return Optional.empty();
        }

        String industry = extractText(
                document, ".company-industry"
        );

        String location = extractText(
                document, ".company-location"
        );

        String website = extractAttribute(
                document, ".company-website", "href"
        );

        website = validWebsite(website);

        return Optional.of(
                new Company(name, website, industry, location)
        );
    }

    private String extractText(
            Document document,
            String selector
    ) {
        Element element = document.selectFirst(selector);

        if (element == null) {
            return null;
        }

        String text = element.text().trim();

        return text.isEmpty() ? null : text;
    }

    private String extractAttribute(
            Document document,
            String selector,
            String attribute
    ) {
        Element element = document.selectFirst(selector);

        if (element == null) {
            return null;
        }

        String value = element.attr(attribute).trim();

        return value.isEmpty() ? null : value;
    }
    private String validWebsite(String value) {
        if (value == null) {
            return null;
        }
        try {
            URI uri = URI.create(value);
            boolean web = "https".equalsIgnoreCase(uri.getScheme())
                    || "http".equalsIgnoreCase(uri.getScheme());
            return web && uri.getHost() != null ? value : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
