package com.studentcareer.opportunity.service;

import com.studentcareer.common.exception.InvalidRequestException;
import com.studentcareer.opportunity.model.*;
import com.studentcareer.opportunity.model.OpportunityTypes.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;

public final class OpportunityRules {
    public static final ZoneId ZONE = ZoneId.of("Indian/Mauritius");
    private OpportunityRules() { }

    public static OpportunityDraft draft(OpportunityInput input, Instant now) {
        if (input == null || input.companyId() == null || input.type() == null) {
            throw invalid("companyId and type are required");
        }
        String title = text(input.title(), "title", 200, true);
        String description = text(input.description(), "description", 20000, true);
        if (input.publishedOn() != null && input.publishedOn().isAfter(now.atZone(ZONE).toLocalDate())) {
            throw invalid("publishedOn cannot be in the future");
        }
        Deadline deadline = deadline(input.deadline());
        if (input.publishedOn() != null && deadline.at() != null
                && deadline.at().atZone(ZONE).toLocalDate().isBefore(input.publishedOn())) {
            throw invalid("deadline cannot precede publishedOn");
        }
        Eligibility local = input.mauritianEligibility() == null ? Eligibility.UNKNOWN : input.mauritianEligibility();
        Eligibility international = input.internationalEligibility() == null ? Eligibility.UNKNOWN : input.internationalEligibility();
        String eligibility = text(input.eligibilityDetails(), "eligibilityDetails", 2000, false);
        if ((local != Eligibility.UNKNOWN || international != Eligibility.UNKNOWN) && eligibility == null) {
            throw invalid("Explicit eligibility requires source-based eligibilityDetails");
        }
        return new OpportunityDraft(input.companyId(), title, input.type(), description,
                text(input.sector(), "sector", 200, false), text(input.location(), "location", 300, false),
                input.workMode() == null ? WorkMode.UNKNOWN : input.workMode(), input.publishedOn(),
                deadline.at(), deadline.precision(), url(input.sourceUrl()),
                text(input.sourceTitle(), "sourceTitle", 300, true),
                text(input.externalReference(), "externalReference", 128, false),
                input.applicationUrl() == null || input.applicationUrl().isBlank() ? null : url(input.applicationUrl()),
                local, international, eligibility);
    }

    public static Status status(OpportunitySnapshot opportunity, Instant now) {
        if (opportunity.verification() == Verification.REJECTED) { return Status.REJECTED; }
        if (opportunity.closed()) { return Status.CLOSED; }
        if (opportunity.details().deadlineAt() != null && !opportunity.details().deadlineAt().isAfter(now)) {
            return Status.EXPIRED;
        }
        if (opportunity.verification() != Verification.CONFIRMED || opportunity.lastVerifiedAt() == null) {
            return Status.UNVERIFIED;
        }
        return !opportunity.lastVerifiedAt().plus(30, ChronoUnit.DAYS).isAfter(now) ? Status.STALE : Status.OPEN;
    }

    public static String duplicateKey(OpportunityDraft draft) {
        String discriminator = draft.externalReference() == null ? normalize(draft.title()) : draft.externalReference();
        String value = draft.companyId() + "\n" + draft.sourceUrl() + "\n" + discriminator;
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }

    public static String normalize(String value) {
        return com.studentcareer.common.text.SearchText.normalize(value);
    }

    public static String text(String value, String name, int max, boolean required) {
        if (value == null || value.isBlank()) {
            if (required) { throw invalid(name + " is required"); }
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > max) { throw invalid(name + " is too long (maximum " + max + ")"); }
        return trimmed;
    }

    public static String url(String value) {
        text(value, "source/application URL", 2048, true);
        try {
            URI uri = URI.create(value.strip()).normalize();
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("https") && !scheme.equals("http")) || uri.getHost() == null || uri.getUserInfo() != null) {
                throw invalid("URL must be absolute HTTP(S), with a host and without credentials");
            }
            int port = uri.getPort();
            if ((scheme.equals("https") && port == 443) || (scheme.equals("http") && port == 80)) { port = -1; }
            String authority = uri.getHost().toLowerCase(Locale.ROOT) + (port < 0 ? "" : ":" + port);
            String path = uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath();
            return new URI(scheme + "://" + authority + path + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())).toASCIIString();
        } catch (IllegalArgumentException | URISyntaxException exception) {
            throw invalid("URL must be a valid absolute HTTP(S) URL");
        }
    }

    public static Deadline deadline(String value) {
        if (value == null || value.isBlank()) { return new Deadline(null, DeadlinePrecision.UNKNOWN); }
        try {
            if (value.strip().matches("\\d{4}-\\d{2}-\\d{2}")) {
                Instant at = LocalDate.parse(value.strip()).atTime(23, 59, 59, 999999000).atZone(ZONE).toInstant();
                return new Deadline(at, DeadlinePrecision.DATE);
            }
            return new Deadline(OffsetDateTime.parse(value.strip()).toInstant().truncatedTo(ChronoUnit.MICROS), DeadlinePrecision.TIME);
        } catch (DateTimeException exception) { throw invalid("deadline must be an ISO date or date-time with offset"); }
    }

    public static InvalidRequestException invalid(String message) { return new InvalidRequestException(message); }
    public record Deadline(Instant at, DeadlinePrecision precision) { }
}
