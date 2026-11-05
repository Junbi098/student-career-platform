package com.studentcareer.opportunity.service;

import com.studentcareer.common.exception.InvalidRequestException;
import com.studentcareer.opportunity.model.*;
import com.studentcareer.opportunity.model.OpportunityTypes.*;
import com.studentcareer.support.OpportunityTestData;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OpportunityRulesTest {
    private final Instant now = Instant.parse("2026-11-05T12:00:00Z");
    private final UUID company = UUID.randomUUID();
    private final OpportunityInput input = OpportunityTestData.input(company,"a","Java internship",Eligibility.UNKNOWN);
    private OpportunitySnapshot snapshot(Verification verification, boolean closed, Instant deadline, Instant verified) {
        var source = OpportunityRules.draft(input,now);
        var details = new OpportunityDraft(source.companyId(),source.title(),source.type(),source.description(),source.sector(),source.location(),
                source.workMode(),source.publishedOn(),deadline,deadline==null?DeadlinePrecision.UNKNOWN:DeadlinePrecision.TIME,
                source.sourceUrl(),source.sourceTitle(),source.externalReference(),source.applicationUrl(),source.mauritianEligibility(),source.internationalEligibility(),source.eligibilityDetails());
        return new OpportunitySnapshot(UUID.randomUUID(),0,details,Origin.MANUAL_ENTRY,verification,closed,now,now,verified);
    }
    @Test void shouldKeepUnspecifiedEligibilityUnknown() {
        var draft=OpportunityRules.draft(input,now);
        assertEquals(Eligibility.UNKNOWN,draft.mauritianEligibility());
        assertEquals(Eligibility.UNKNOWN,draft.internationalEligibility());
    }
    @Test void shouldInterpretDateOnlyDeadlineAtEndOfMauritiusDay() {
        var deadline=OpportunityRules.deadline("2026-11-05");
        assertEquals(Instant.parse("2026-11-05T19:59:59.999999Z"),deadline.at());
        assertEquals(DeadlinePrecision.DATE,deadline.precision());
    }
    @Test void shouldPreserveExplicitDeadlineOffset() {
        assertEquals(Instant.parse("2026-11-05T08:00:00Z"),OpportunityRules.deadline("2026-11-05T12:00:00+04:00").at());
    }
    @Test void shouldExpireAtDeadlineWithoutScheduledTask() {
        assertEquals(Status.EXPIRED,OpportunityRules.status(snapshot(Verification.CONFIRMED,false,now,now.minusSeconds(1)),now));
    }
    @Test void shouldRequireFreshConfirmationToBeOpen() {
        assertEquals(Status.UNVERIFIED,OpportunityRules.status(snapshot(Verification.UNVERIFIED,false,null,null),now));
        assertEquals(Status.OPEN,OpportunityRules.status(snapshot(Verification.CONFIRMED,false,null,now),now));
        assertEquals(Status.STALE,OpportunityRules.status(snapshot(Verification.CONFIRMED,false,null,now.minusSeconds(30L*86400)),now));
    }
    @Test void shouldKeepManualClosureAndRejectionDistinct() {
        assertEquals(Status.CLOSED,OpportunityRules.status(snapshot(Verification.CONFIRMED,true,null,now),now));
        assertEquals(Status.REJECTED,OpportunityRules.status(snapshot(Verification.REJECTED,false,null,now),now));
    }
    @Test void shouldRejectUnsupportedUrlsAndMalformedDates() {
        for(String value:new String[]{"javascript:alert(1)","/relative","https://user:secret@example.com","not a URL"}) {
            assertThrows(InvalidRequestException.class,()->OpportunityRules.url(value));
        }
        assertThrows(InvalidRequestException.class,()->OpportunityRules.deadline("bad-date"));
    }
    @Test void shouldCanonicalizeSourceForDuplicateDetection() {
        assertEquals("https://example.com/jobs?id=1",OpportunityRules.url("https://EXAMPLE.com:443/jobs?id=1#application"));
        assertNotEquals(OpportunityRules.url("https://example.com/?id=1"),OpportunityRules.url("https://example.com/?id=2"));
    }
    @Test void shouldNotLoseKnownReferenceWhenTitleChanges() {
        assertEquals(OpportunityRules.duplicateKey(OpportunityRules.draft(input,now)),
                OpportunityRules.duplicateKey(OpportunityRules.draft(OpportunityTestData.title(input,"Updated internship"),now)));
    }
    @Test void shouldRequireCompanyTypeAndTitle() {
        assertThrows(InvalidRequestException.class,()->OpportunityRules.draft(null,now));
        assertThrows(InvalidRequestException.class,()->OpportunityRules.draft(OpportunityTestData.title(input,"  "),now));
    }
    @Test void shouldRejectFuturePublicationAndEarlierDeadline() {
        var future=new OpportunityInput(company,input.title(),input.type(),input.description(),input.sector(),input.location(),input.workMode(),
                java.time.LocalDate.of(2099,1,1),"2099-12-31",input.sourceUrl(),input.sourceTitle(),input.externalReference(),null,
                Eligibility.UNKNOWN,Eligibility.UNKNOWN,null);
        assertThrows(InvalidRequestException.class,()->OpportunityRules.draft(future,now));
        var earlier=new OpportunityInput(company,input.title(),input.type(),input.description(),null,null,null,
                java.time.LocalDate.of(2026,1,1),"2025-12-31",input.sourceUrl(),input.sourceTitle(),null,null,null,null,null);
        assertThrows(InvalidRequestException.class,()->OpportunityRules.draft(earlier,now));
    }
    @Test void shouldRequireSourceBasedDetailsForKnownEligibility() {
        var unsupported=new OpportunityInput(company,input.title(),input.type(),input.description(),null,null,null,null,null,
                input.sourceUrl(),input.sourceTitle(),null,null,Eligibility.YES,Eligibility.UNKNOWN,null);
        assertThrows(InvalidRequestException.class,()->OpportunityRules.draft(unsupported,now));
    }
}
