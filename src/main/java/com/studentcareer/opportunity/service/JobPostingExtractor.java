package com.studentcareer.opportunity.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.opportunity.model.OpportunityInput;
import com.studentcareer.opportunity.model.OpportunityTypes.*;
import com.studentcareer.opportunity.service.OpportunityContracts.HtmlImport;
import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.jsoup.Jsoup;

/** Parses submitted JSON-LD only. No URL or JSON-LD context is fetched. */
public class JobPostingExtractor {
    private final ObjectMapper mapper = new ObjectMapper();

    public Extraction extract(HtmlImport request, String expectedCompanyName) {
        if (request == null || request.html() == null || request.html().length() > 262144) {
            throw OpportunityRules.invalid("html is required and limited to 262144 characters");
        }
        String sourceUrl = OpportunityRules.url(request.sourceUrl());
        String sourceTitle = OpportunityRules.text(request.sourceTitle(), "sourceTitle", 300, true);
        List<JsonNode> jobs = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int script = 0;
        for (var element : Jsoup.parse(request.html()).select("script[type=application/ld+json]")) {
            script++;
            if (script > 30) { throw OpportunityRules.invalid("Too many JSON-LD scripts"); }
            try {
                collect(mapper.readTree(element.data()), jobs, 0, new int[]{0});
            } catch (IOException exception) {
                warnings.add("JSON-LD script " + script + " is malformed");
            }
        }
        if (jobs.size() > 25) { throw OpportunityRules.invalid("At most 25 JobPosting entries per submission"); }
        List<Candidate> candidates = new ArrayList<>();
        for (JsonNode job : jobs) {
            String title = job.path("title").asText("");
            try {
                String employer = job.path("hiringOrganization").path("name").asText("");
                if (!OpportunityRules.normalize(employer).equals(OpportunityRules.normalize(expectedCompanyName))) {
                    throw OpportunityRules.invalid("hiringOrganization.name must match the selected company dossier");
                }
                Type type = internship(job.path("employmentType")) ? Type.INTERNSHIP :
                        OpportunityRules.normalize(title).matches(".*\\bjunior\\b.*") ? Type.JUNIOR_JOB : null;
                if (type == null) { throw OpportunityRules.invalid("No explicit internship or junior role identified"); }
                String description = Jsoup.parse(job.path("description").asText("")).text();
                String sector = job.path("industry").isTextual() ? job.path("industry").asText() : null;
                JsonNode place = job.path("jobLocation");
                if (place.isArray() && !place.isEmpty()) { place = place.get(0); }
                JsonNode address = place.path("address");
                String city = address.path("addressLocality").asText("");
                String country = address.path("addressCountry").isTextual() ? address.path("addressCountry").asText() :
                        address.path("addressCountry").path("name").asText("");
                String location = String.join(", ", java.util.stream.Stream.of(city, country).filter(value -> !value.isBlank()).toList());
                WorkMode mode = "TELECOMMUTE".equals(job.path("jobLocationType").asText()) ? WorkMode.REMOTE : WorkMode.UNKNOWN;
                String posted = job.path("datePosted").asText("");
                LocalDate date = posted.isBlank() ? null : posted.matches("\\d{4}-\\d{2}-\\d{2}")
                        ? LocalDate.parse(posted) : OffsetDateTime.parse(posted).atZoneSameInstant(OpportunityRules.ZONE).toLocalDate();
                JsonNode identifier = job.path("identifier");
                String reference = identifier.isTextual() ? identifier.asText() : identifier.path("value").asText(null);
                String application = job.path("url").isTextual() ? job.path("url").asText() : null;
                String details = job.path("eligibilityToWorkRequirement").isTextual()
                        ? job.path("eligibilityToWorkRequirement").asText() : null;
                var input = new OpportunityInput(request.companyId(), title, type, description, sector, location, mode,
                        date, job.path("validThrough").asText(null), sourceUrl, sourceTitle, reference, application,
                        Eligibility.UNKNOWN, Eligibility.UNKNOWN, details);
                candidates.add(new Candidate(title, input, null));
            } catch (IllegalArgumentException | java.time.DateTimeException | com.studentcareer.common.exception.InvalidRequestException exception) {
                candidates.add(new Candidate(title.length() > 200 ? title.substring(0, 200) : title, null,
                        exception.getMessage() == null ? "Unsupported JobPosting data" : exception.getMessage()));
            }
        }
        return new Extraction(List.copyOf(candidates), List.copyOf(warnings));
    }

    private void collect(JsonNode node, List<JsonNode> jobs, int depth, int[] visited) {
        if (node == null) { return; }
        if (depth > 16 || ++visited[0] > 1000) { throw OpportunityRules.invalid("JSON-LD graph is too complex"); }
        if (node.isArray()) { for (var child : node) { collect(child, jobs, depth + 1, visited); } return; }
        if (!node.isObject()) { return; }
        JsonNode type = node.path("@type");
        boolean job = isJob(type.asText());
        if (type.isArray()) { for (var item : type) { job |= isJob(item.asText()); } }
        if (job) { jobs.add(node); }
        if (node.has("@graph")) { collect(node.path("@graph"), jobs, depth + 1, visited); }
    }
    private boolean internship(JsonNode type) {
        if (type.isArray()) { for (var value : type) { if (internship(value)) { return true; } } return false; }
        String value = type.asText("").strip().toUpperCase(Locale.ROOT);
        return value.equals("INTERN") || value.equals("INTERNSHIP");
    }
    private boolean isJob(String type) {
        return "JobPosting".equals(type) || "https://schema.org/JobPosting".equals(type) || "http://schema.org/JobPosting".equals(type);
    }
    public record Candidate(String title, OpportunityInput input, String error) { }
    public record Extraction(List<Candidate> candidates, List<String> warnings) { }
}
