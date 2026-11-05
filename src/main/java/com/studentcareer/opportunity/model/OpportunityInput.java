package com.studentcareer.opportunity.model;

import java.time.LocalDate;
import java.util.UUID;
import com.studentcareer.opportunity.model.OpportunityTypes.*;

/** Submitted claims, not proof that the server visited the declared source. */
public record OpportunityInput(UUID companyId, String title, Type type, String description,
        String sector, String location, WorkMode workMode, LocalDate publishedOn, String deadline,
        String sourceUrl, String sourceTitle, String externalReference, String applicationUrl,
        Eligibility mauritianEligibility, Eligibility internationalEligibility, String eligibilityDetails) { }
