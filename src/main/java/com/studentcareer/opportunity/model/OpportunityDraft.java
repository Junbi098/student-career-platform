package com.studentcareer.opportunity.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.studentcareer.opportunity.model.OpportunityTypes.*;

public record OpportunityDraft(UUID companyId, String title, Type type, String description,
        String sector, String location, WorkMode workMode, LocalDate publishedOn,
        Instant deadlineAt, DeadlinePrecision deadlinePrecision, String sourceUrl, String sourceTitle,
        String externalReference, String applicationUrl, Eligibility mauritianEligibility,
        Eligibility internationalEligibility, String eligibilityDetails) { }
