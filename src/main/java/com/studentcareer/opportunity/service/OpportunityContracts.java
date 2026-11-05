package com.studentcareer.opportunity.service;

import com.studentcareer.opportunity.model.*;
import com.studentcareer.opportunity.model.OpportunityTypes.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OpportunityContracts {
    private OpportunityContracts() { }
    public record Update(Long version, OpportunityInput opportunity) { }
    public record Review(Long version, ReviewAction action, Boolean sourceReviewed, String evidenceUrl, String notes) { }
    public record HtmlImport(UUID companyId, String html, String sourceUrl, String sourceTitle) { }
    public record CompanySummary(UUID id, String name) { }
    public record View(UUID id, long version, CompanySummary company, OpportunityDraft details,
            Status status, Verification verification, Origin origin, boolean sourceFetchedByServer,
            Instant sourceAccessedAt, Instant firstSubmittedAt, Instant lastSubmittedAt, Instant lastVerifiedAt,
            int relevanceScore, List<String> matchedCriteria, Eligibility selectedStudentEligibility) { }
    public record Page(List<View> items, int page, int size, long totalElements, int totalPages) { }
    public record Query(String q, String sector, String location, Type type, WorkMode workMode,
            StudentType studentType, boolean includeUnknownEligibility, Status status, boolean verifiedOnly,
            boolean includeInactive, int page, int size, Sort sort) { }
    public record ImportItem(String title, UUID id, String outcome, String message) { }
    public record ImportReport(int detected, int created, int updated, int unchanged, int rejected,
            List<ImportItem> results, List<String> warnings) { }
}
