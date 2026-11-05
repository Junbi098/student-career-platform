package com.studentcareer.support;

import com.studentcareer.opportunity.model.OpportunityInput;
import com.studentcareer.opportunity.model.OpportunityTypes.*;
import java.time.LocalDate;
import java.util.UUID;

public final class OpportunityTestData {
    private OpportunityTestData() { }
    public static OpportunityInput input(UUID company, String reference, String title, Eligibility international) {
        return new OpportunityInput(company, title, Type.INTERNSHIP, "Synthetic Java development internship for automated testing.",
                "Software", "Ébène, Mauritius", WorkMode.HYBRID, LocalDate.of(2020,1,1), "2099-12-31",
                "https://example.test/jobs/" + reference, "Synthetic test source", reference,
                "https://example.test/apply/" + reference, Eligibility.UNKNOWN, international,
                international == Eligibility.UNKNOWN ? null : "Synthetic employer statement used only in isolated tests");
    }
    public static OpportunityInput title(OpportunityInput input, String title) {
        return new OpportunityInput(input.companyId(), title, input.type(), input.description(), input.sector(), input.location(),
                input.workMode(), input.publishedOn(), input.deadline(), input.sourceUrl(), input.sourceTitle(),
                input.externalReference(), input.applicationUrl(), input.mauritianEligibility(), input.internationalEligibility(), input.eligibilityDetails());
    }
    public static String html(String companyName, String title, String reference) {
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        var job = mapper.createObjectNode();
        job.put("@type", "JobPosting"); job.put("title", title); job.put("description", "<p>Synthetic Java role.</p>");
        job.put("employmentType", "INTERN"); job.put("validThrough", "2099-12-31");
        job.putObject("identifier").put("value", reference);
        job.putObject("hiringOrganization").put("name", companyName);
        return "<script type='application/ld+json'>" + job + "</script>";
    }
}
