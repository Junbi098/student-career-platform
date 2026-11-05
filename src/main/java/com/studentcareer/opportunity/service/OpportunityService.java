package com.studentcareer.opportunity.service;

import com.studentcareer.company.domain.CompanyCatalogEntry;
import com.studentcareer.company.exception.CompanyNotFoundException;
import com.studentcareer.company.repository.CompanyRepository;
import com.studentcareer.opportunity.model.*;
import com.studentcareer.opportunity.model.OpportunityTypes.*;
import com.studentcareer.opportunity.persistence.*;
import com.studentcareer.opportunity.service.OpportunityContracts.*;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityService {
    private final OpportunityJpaRepository opportunities;
    private final OpportunityReviewJpaRepository reviews;
    private final CompanyRepository companies;
    private final Clock clock;

    public OpportunityService(OpportunityJpaRepository opportunities, OpportunityReviewJpaRepository reviews,
                              CompanyRepository companies, Clock clock) {
        this.opportunities = opportunities;
        this.reviews = reviews;
        this.companies = companies;
        this.clock = clock;
    }

    @Transactional
    public View create(OpportunityInput input) {
        Instant now = now();
        OpportunityDraft draft = OpportunityRules.draft(input, now);
        CompanyCatalogEntry company = company(draft.companyId());
        String key = OpportunityRules.duplicateKey(draft);
        if (opportunities.findByDuplicateKey(key).isPresent()) {
            throw new OpportunityConflictException("This company/source/reference or title is already recorded");
        }
        OpportunityEntity entity = opportunities.saveAndFlush(OpportunityEntity.create(draft, key, Origin.MANUAL_ENTRY, now));
        record(entity.snapshot(), ReviewAction.SUBMITTED, "Manually submitted; source not fetched by server", null, false, now);
        return view(entity.snapshot(), company, now, null);
    }

    @Transactional
    public View update(UUID id, Update request) {
        if (request == null) { throw OpportunityRules.invalid("Update body is required"); }
        OpportunityEntity entity = entity(id);
        OpportunitySnapshot before = entity.snapshot();
        version(request.version(), before.version());
        OpportunityDraft draft = OpportunityRules.draft(request.opportunity(), now());
        if (!draft.companyId().equals(before.details().companyId())) {
            throw OpportunityRules.invalid("An opportunity cannot be reassigned to another company dossier");
        }
        String key = OpportunityRules.duplicateKey(draft);
        opportunities.findByDuplicateKey(key).filter(other -> !other.snapshot().id().equals(id)).ifPresent(other -> {
            throw new OpportunityConflictException("The revised source/reference duplicates another opportunity");
        });
        Instant now = now();
        boolean changed = !draft.equals(before.details());
        entity.apply(draft, key, before.origin(), now);
        if (changed) { entity.resetVerification(); }
        opportunities.flush();
        if (changed) { record(entity.snapshot(), ReviewAction.MATERIAL_CHANGE,
                "Content changed; previous confirmation invalidated", draft.sourceUrl(), false, now); }
        return view(entity.snapshot(), company(draft.companyId()), now, null);
    }

    @Transactional
    public View review(UUID id, Review request) {
        if (request == null || request.action() == null) { throw OpportunityRules.invalid("Review action is required"); }
        OpportunityEntity entity = entity(id);
        OpportunitySnapshot before = entity.snapshot();
        version(request.version(), before.version());
        String notes = OpportunityRules.text(request.notes(), "notes", 2000, true);
        if (notes.length() < 10) { throw OpportunityRules.invalid("Review notes must explain the decision (at least 10 characters)"); }
        Instant now = now();
        ReviewAction action = request.action();
        if (action == ReviewAction.MATERIAL_CHANGE || action == ReviewAction.SUBMITTED) {
            throw OpportunityRules.invalid("This action is reserved for the import/update workflow");
        }
        String evidence = null;
        if (action == ReviewAction.CONFIRM || action == ReviewAction.REJECT) {
            if (!Boolean.TRUE.equals(request.sourceReviewed())) { throw OpportunityRules.invalid("Explicitly acknowledge reviewing the source"); }
            evidence = OpportunityRules.url(request.evidenceUrl());
            if (!evidence.equals(before.details().sourceUrl())) {
                throw OpportunityRules.invalid("Evidence URL must match the declared opportunity source");
            }
        }
        if ((action == ReviewAction.CONFIRM || action == ReviewAction.REOPEN)
                && before.details().deadlineAt() != null && !before.details().deadlineAt().isAfter(now)) {
            throw new OpportunityConflictException("An expired opportunity cannot be confirmed or reopened");
        }
        if (action == ReviewAction.CONFIRM && before.closed()) {
            throw new OpportunityConflictException("Reopen the opportunity before confirming it");
        }
        entity.review(action, now);
        opportunities.flush();
        record(entity.snapshot(), action, notes, evidence, Boolean.TRUE.equals(request.sourceReviewed()), now);
        return view(entity.snapshot(), company(before.details().companyId()), now, null);
    }

    @Transactional(readOnly = true)
    public View find(UUID id) {
        var snapshot = entity(id).snapshot();
        return view(snapshot, company(snapshot.details().companyId()), now(), null);
    }

    @Transactional(readOnly = true)
    public List<OpportunityReviewEntity.ReviewRecord> history(UUID id) {
        entity(id);
        return reviews.findByOpportunityIdOrderByIdAsc(id).stream().map(OpportunityReviewEntity::snapshot).toList();
    }

    @Transactional(readOnly = true)
    public Page search(Query query) {
        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            throw OpportunityRules.invalid("page must be >= 0 and size between 1 and 100");
        }
        String q = filter(query.q());
        String sector = filter(query.sector());
        String location = filter(query.location());
        Instant now = now();
        Map<UUID, CompanyCatalogEntry> companyMap = companies.findAll().stream()
                .collect(Collectors.toMap(CompanyCatalogEntry::id, Function.identity()));
        List<View> results = new ArrayList<>();
        for (OpportunityEntity entity : opportunities.findAllByOrderByFirstSubmittedAtDescIdAsc()) {
            OpportunitySnapshot opportunity = entity.snapshot();
            var details = opportunity.details();
            Status status = OpportunityRules.status(opportunity, now);
            if (query.status() != null ? status != query.status() :
                    (!query.includeInactive() && (status == Status.EXPIRED || status == Status.CLOSED || status == Status.REJECTED))) { continue; }
            if (query.verifiedOnly() && status != Status.OPEN) { continue; }
            if (query.type() != null && details.type() != query.type()) { continue; }
            if (query.workMode() != null && details.workMode() != query.workMode()) { continue; }
            if (!contains(details.sector(), sector) || !contains(details.location(), location)) { continue; }
            var company = companyMap.get(details.companyId());
            if (company == null) { throw new IllegalStateException("Missing associated company dossier"); }
            String bag = OpportunityRules.normalize(details.title() + " " + details.description() + " " + company.dossier().getCompany().getName());
            if (!q.isEmpty() && !Arrays.stream(q.split(" ")).allMatch(bag::contains)) { continue; }
            Eligibility eligibility = eligibility(details, query.studentType());
            if (query.studentType() != null && eligibility != Eligibility.YES
                    && !(query.includeUnknownEligibility() && eligibility == Eligibility.UNKNOWN)) { continue; }
            results.add(view(opportunity, company, now, query));
        }
        Comparator<View> comparator = switch (query.sort() == null ? Sort.RELEVANCE : query.sort()) {
            case RELEVANCE -> Comparator.comparingInt(View::relevanceScore).reversed()
                    .thenComparing(View::lastSubmittedAt, Comparator.reverseOrder());
            case NEWEST -> Comparator.comparing(View::firstSubmittedAt, Comparator.reverseOrder());
            case CLOSING_SOON -> Comparator.comparing(view -> view.details().deadlineAt(), Comparator.nullsLast(Comparator.naturalOrder()));
        };
        results.sort(comparator.thenComparing(View::id));
        int from = (int) Math.min((long) query.page() * query.size(), results.size());
        int to = Math.min(from + query.size(), results.size());
        return new Page(List.copyOf(results.subList(from, to)), query.page(), query.size(), results.size(),
                (int) ((results.size() + (long) query.size() - 1) / query.size()));
    }

    // Used by the structured HTML importer within its enclosing transaction.
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.MANDATORY)
    public ImportItem importDraft(OpportunityDraft draft) {
        company(draft.companyId());
        String key = OpportunityRules.duplicateKey(draft);
        Instant now = now();
        var existing = opportunities.findByDuplicateKey(key);
        OpportunityEntity entity;
        String outcome;
        if (existing.isEmpty()) {
            entity = OpportunityEntity.create(draft, key, Origin.HTML_SUBMISSION, now);
            outcome = "CREATED";
        } else {
            entity = existing.orElseThrow();
            if (entity.snapshot().details().equals(draft)) { outcome = "UNCHANGED"; }
            else { outcome = "UPDATED"; entity.resetVerification(); }
            entity.apply(draft, key, Origin.HTML_SUBMISSION, now);
        }
        entity = opportunities.saveAndFlush(entity);
        if (!outcome.equals("UNCHANGED")) {
            record(entity.snapshot(), outcome.equals("CREATED") ? ReviewAction.SUBMITTED : ReviewAction.MATERIAL_CHANGE,
                    "Structured HTML submitted; source not fetched by server", draft.sourceUrl(), false, now);
        }
        return new ImportItem(draft.title(), entity.snapshot().id(), outcome, null);
    }

    private View view(OpportunitySnapshot opportunity, CompanyCatalogEntry company, Instant now, Query query) {
        var draft = opportunity.details();
        List<String> reasons = new ArrayList<>();
        int score = 0;
        if (query != null) {
            String q = OpportunityRules.normalize(query.q());
            if (!q.isEmpty()) {
                for (String token : q.split(" ")) {
                    if (OpportunityRules.normalize(draft.title()).contains(token)) { score += 3; }
                    else if (OpportunityRules.normalize(draft.description()).contains(token)) { score++; }
                }
                reasons.add("KEYWORDS");
            }
            if (query.sector() != null && !query.sector().isBlank()) { score += 2; reasons.add("SECTOR"); }
            if (query.location() != null && !query.location().isBlank()) { score += 2; reasons.add("LOCATION"); }
            if (query.studentType() != null && eligibility(draft, query.studentType()) == Eligibility.YES) {
                score += 2; reasons.add("DECLARED_ELIGIBILITY");
            }
        }
        return new View(opportunity.id(), opportunity.version(),
                new CompanySummary(company.id(), company.dossier().getCompany().getName()), draft,
                OpportunityRules.status(opportunity, now), opportunity.verification(), opportunity.origin(), false, null,
                opportunity.firstSubmittedAt(), opportunity.lastSubmittedAt(), opportunity.lastVerifiedAt(),
                score, List.copyOf(reasons), eligibility(draft, query == null ? null : query.studentType()));
    }

    private Eligibility eligibility(OpportunityDraft draft, StudentType student) {
        return student == null ? Eligibility.UNKNOWN : student == StudentType.MAURITIAN
                ? draft.mauritianEligibility() : draft.internationalEligibility();
    }
    private boolean contains(String value, String filter) { return filter.isEmpty() || OpportunityRules.normalize(value).contains(filter); }
    private String filter(String value) { return OpportunityRules.normalize(OpportunityRules.text(value, "filter", 200, false)); }
    private void version(Long expected, long current) {
        if (expected == null || expected < 0) { throw OpportunityRules.invalid("The current nonnegative version is required"); }
        if (expected != current) { throw new OpportunityConflictException("Opportunity changed: reload it before editing/reviewing"); }
    }
    private CompanyCatalogEntry company(UUID id) { return companies.findById(id).orElseThrow(CompanyNotFoundException::new); }
    private OpportunityEntity entity(UUID id) { return opportunities.findById(id).orElseThrow(OpportunityNotFoundException::new); }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private void record(OpportunitySnapshot opportunity, ReviewAction action, String notes, String evidence, boolean reviewed, Instant now) {
        reviews.save(new OpportunityReviewEntity(opportunity.id(), action, notes, evidence, reviewed, now, opportunity.version()));
    }
}
