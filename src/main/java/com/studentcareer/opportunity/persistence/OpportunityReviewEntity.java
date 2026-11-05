package com.studentcareer.opportunity.persistence;

import com.studentcareer.opportunity.model.OpportunityTypes.ReviewAction;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "opportunity_reviews")
public class OpportunityReviewEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "opportunity_id", nullable = false)
    private UUID opportunityId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewAction action;
    @Column(nullable = false, length = 2000)
    private String notes;
    @Column(name = "evidence_url", columnDefinition = "text")
    private String evidenceUrl;
    @Column(name = "source_reviewed", nullable = false)
    private boolean sourceReviewed;
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
    @Column(name = "opportunity_version", nullable = false)
    private long opportunityVersion;

    protected OpportunityReviewEntity() { }

    public OpportunityReviewEntity(UUID opportunityId, ReviewAction action, String notes, String evidenceUrl,
                                  boolean sourceReviewed, Instant now, long version) {
        this.opportunityId = opportunityId;
        this.action = action;
        this.notes = notes;
        this.evidenceUrl = evidenceUrl;
        this.sourceReviewed = sourceReviewed;
        this.recordedAt = now;
        this.opportunityVersion = version;
    }

    public ReviewRecord snapshot() {
        return new ReviewRecord(id, action, notes, evidenceUrl, sourceReviewed, recordedAt, opportunityVersion);
    }

    public record ReviewRecord(long id, ReviewAction action, String notes, String evidenceUrl,
                               boolean sourceReviewed, Instant recordedAt, long opportunityVersion) { }
}
