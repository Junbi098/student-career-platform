package com.studentcareer.opportunity.model;

import java.time.Instant;
import java.util.UUID;
import com.studentcareer.opportunity.model.OpportunityTypes.*;

public record OpportunitySnapshot(UUID id, long version, OpportunityDraft details, Origin origin,
        Verification verification, boolean closed, Instant firstSubmittedAt,
        Instant lastSubmittedAt, Instant lastVerifiedAt) { }
