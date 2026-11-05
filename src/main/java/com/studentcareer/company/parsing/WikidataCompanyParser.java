package com.studentcareer.company.parsing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.company.domain.Company;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Extracts only explicit non-deprecated, non-ended claims from a single curated company entity. */
public class WikidataCompanyParser implements CompanyContentParser {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Optional<Company> parse(String content) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        try {
            JsonNode entities = mapper.readTree(content).path("entities");
            if (!entities.isObject() || entities.size() != 1) {
                throw new IllegalArgumentException("Expected one Wikidata entity");
            }
            JsonNode entity = entities.elements().next();
            if (entity.has("missing")) {
                return Optional.empty();
            }
            String name = label(entity, "en");
            if (name == null) {
                name = label(entity, "fr");
            }
            boolean mauritius = claims(entity, "P17").stream()
                    .anyMatch(value -> "Q1027".equals(value.path("id").asText()));
            if (name == null || !mauritius || !claims(entity, "P576").isEmpty()) {
                return Optional.empty();
            }
            String website = claims(entity, "P856").stream().filter(JsonNode::isTextual)
                    .map(JsonNode::asText).map(String::strip).filter(this::validWebsite).sorted().findFirst().orElse(null);
            // P452 and P159 contain entity IDs, not readable sector/city names. Do not guess labels.
            return Optional.of(new Company(name, website, null, "Mauritius"));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Malformed Wikidata JSON", exception);
        }
    }

    private String label(JsonNode entity, String language) {
        String value = entity.path("labels").path(language).path("value").asText("").strip();
        return value.isEmpty() ? null : value;
    }

    private List<JsonNode> claims(JsonNode entity, String property) {
        List<JsonNode> normal = new ArrayList<>();
        List<JsonNode> preferred = new ArrayList<>();
        for (JsonNode statement : entity.path("claims").path(property)) {
            if ("deprecated".equals(statement.path("rank").asText())
                    || statement.path("qualifiers").has("P582")) {
                continue;
            }
            JsonNode snak = statement.path("mainsnak");
            if (!"value".equals(snak.path("snaktype").asText()) || !snak.path("datavalue").has("value")) {
                continue;
            }
            ("preferred".equals(statement.path("rank").asText()) ? preferred : normal)
                    .add(snak.path("datavalue").path("value"));
        }
        return preferred.isEmpty() ? normal : preferred;
    }

    private boolean validWebsite(String value) {
        try {
            URI uri = URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null && uri.getUserInfo() == null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
