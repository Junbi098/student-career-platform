package com.studentcareer.opportunity.persistence;

import com.studentcareer.opportunity.model.*;
import com.studentcareer.opportunity.model.OpportunityTypes.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "opportunities")
public class OpportunityEntity {
    @Id
    private UUID id;
    @Version
    private Long version;
    @Column(name = "duplicate_key", nullable = false, unique = true, length = 64)
    private String duplicateKey;
    @Column(name = "company_dossier_id")
    private UUID companyId;
    @Column(name = "title", length = 200)
    private String title;
    @Enumerated(EnumType.STRING)
    @Column(name = "opportunity_type", length = 32)
    private Type type;
    @Column(name = "description", columnDefinition = "text")
    private String description;
    @Column(name = "sector", length = 200)
    private String sector;
    @Column(name = "location", length = 300)
    private String location;
    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", length = 32)
    private WorkMode workMode;
    @Column(name = "published_on")
    private LocalDate publishedOn;
    @Column(name = "deadline_at")
    private Instant deadlineAt;
    @Enumerated(EnumType.STRING)
    @Column(name = "deadline_precision", length = 16)
    private DeadlinePrecision deadlinePrecision;
    @Column(name = "source_url", columnDefinition = "text")
    private String sourceUrl;
    @Column(name = "source_title", length = 300)
    private String sourceTitle;
    @Column(name = "external_reference", length = 128)
    private String externalReference;
    @Column(name = "application_url", columnDefinition = "text")
    private String applicationUrl;
    @Enumerated(EnumType.STRING)
    @Column(name = "mauritian_eligibility", length = 16)
    private Eligibility mauritianEligibility;
    @Enumerated(EnumType.STRING)
    @Column(name = "international_eligibility", length = 16)
    private Eligibility internationalEligibility;
    @Column(name = "eligibility_details", length = 2000)
    private String eligibilityDetails;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Origin origin;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Verification verification;
    private boolean closed;
    @Column(name = "first_submitted_at", nullable = false)
    private Instant firstSubmittedAt;
    @Column(name = "last_submitted_at", nullable = false)
    private Instant lastSubmittedAt;
    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    protected OpportunityEntity() { }

    public static OpportunityEntity create(OpportunityDraft draft, String key, Origin origin, Instant now) {
        OpportunityEntity entity = new OpportunityEntity();
        entity.id = UUID.randomUUID();
        entity.verification = Verification.UNVERIFIED;
        entity.firstSubmittedAt = now;
        entity.apply(draft, key, origin, now);
        return entity;
    }

    public void apply(OpportunityDraft draft, String key, Origin method, Instant now) {
        companyId = draft.companyId();
        title = draft.title();
        type = draft.type();
        description = draft.description();
        sector = draft.sector();
        location = draft.location();
        workMode = draft.workMode();
        publishedOn = draft.publishedOn();
        deadlineAt = draft.deadlineAt();
        deadlinePrecision = draft.deadlinePrecision();
        sourceUrl = draft.sourceUrl();
        sourceTitle = draft.sourceTitle();
        externalReference = draft.externalReference();
        applicationUrl = draft.applicationUrl();
        mauritianEligibility = draft.mauritianEligibility();
        internationalEligibility = draft.internationalEligibility();
        eligibilityDetails = draft.eligibilityDetails();
        duplicateKey = key;
        origin = method;
        lastSubmittedAt = now;
    }

    public void resetVerification() {
        verification = Verification.UNVERIFIED;
        lastVerifiedAt = null;
    }

    public void review(ReviewAction action, Instant now) {
        switch (action) {
            case CONFIRM -> { verification = Verification.CONFIRMED; lastVerifiedAt = now; }
            case REJECT -> { verification = Verification.REJECTED; lastVerifiedAt = now; }
            case CLOSE -> closed = true;
            case REOPEN -> { closed = false; resetVerification(); }
            default -> throw new IllegalArgumentException("Unsupported manual review action");
        }
    }

    public OpportunitySnapshot snapshot() {
        var draft = new OpportunityDraft(companyId, title, type, description, sector, location, workMode, publishedOn, deadlineAt, deadlinePrecision, sourceUrl, sourceTitle, externalReference, applicationUrl, mauritianEligibility, internationalEligibility, eligibilityDetails);
        return new OpportunitySnapshot(id, version == null ? 0 : version, draft, origin, verification, closed,
                firstSubmittedAt, lastSubmittedAt, lastVerifiedAt);
    }
}
