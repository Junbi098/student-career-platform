package com.studentcareer.opportunity.service;

import com.studentcareer.common.exception.InvalidRequestException;
import com.studentcareer.opportunity.model.OpportunityTypes.*;
import com.studentcareer.opportunity.service.OpportunityContracts.HtmlImport;
import com.studentcareer.support.OpportunityTestData;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JobPostingExtractorTest {
    private final JobPostingExtractor extractor=new JobPostingExtractor();
    private final UUID company=UUID.randomUUID();
    private JobPostingExtractor.Extraction extract(String html) {
        return extractor.extract(new HtmlImport(company,html,"https://example.test/jobs","Synthetic source"),"Synthetic Company");
    }
    @Test void shouldDetectExplicitInternshipWithoutGuessingEligibility() {
        var result=extract(OpportunityTestData.html("Synthetic Company","Java internship","R1"));
        var input=result.candidates().getFirst().input();
        assertEquals(Type.INTERNSHIP,input.type());
        assertEquals(Eligibility.UNKNOWN,input.internationalEligibility());
        assertEquals(WorkMode.UNKNOWN,input.workMode());
        assertEquals("Synthetic Java role.",input.description());
    }
    @Test void shouldRejectDifferentEmployer() {
        assertNotNull(extract(OpportunityTestData.html("Other Company","Java internship","R1")).candidates().getFirst().error());
    }
    @Test void shouldNotTreatInternationalAsInternship() {
        String html=OpportunityTestData.html("Synthetic Company","Senior architect","R1").replace("INTERN","INTERNATIONAL");
        assertNotNull(extract(html).candidates().getFirst().error());
    }
    @Test void shouldDetectExplicitJuniorTitleButNotAllFullTimeJobs() {
        String junior=OpportunityTestData.html("Synthetic Company","Junior developer","R1").replace("INTERN","FULL_TIME");
        assertEquals(Type.JUNIOR_JOB,extract(junior).candidates().getFirst().input().type());
        assertNotNull(extract(junior.replace("Junior developer","Senior developer")).candidates().getFirst().error());
    }
    @Test void shouldReportMalformedScriptAndContinueWithValidOne() {
        var result=extract("<script type='application/ld+json'>not-json</script>"+OpportunityTestData.html("Synthetic Company","Internship","R1"));
        assertEquals(1,result.warnings().size()); assertEquals(1,result.candidates().size());
    }
    @Test void shouldNotInventPostingFromOrdinaryCompanyHtml() {
        assertTrue(extract("<h1>Synthetic Company hires someday</h1>").candidates().isEmpty());
    }
    @Test void shouldBoundSubmittedHtmlSize() {
        assertThrows(InvalidRequestException.class,()->extract("X".repeat(262145)));
    }
    @Test void shouldHandleGraphAndTypeArrayWithoutFetchingContext() {
        String html = """
                <script type="application/ld+json">
                {"@context":"https://invalid.invalid/context","@graph":[{"@type":["Thing","JobPosting"],
                "title":"Junior developer","description":"Synthetic role","hiringOrganization":{"name":"Synthetic Company"}}]}
                </script>
                """;
        assertEquals(Type.JUNIOR_JOB,extract(html).candidates().getFirst().input().type());
    }
    @Test void shouldRejectTooManyPostingsInOneDocument() {
        String html=OpportunityTestData.html("Synthetic Company","Java internship","R1").repeat(26);
        assertThrows(InvalidRequestException.class,()->extract(html));
    }
    @Test void shouldNotTurnApplicantLocationIntoCitizenshipEligibility() {
        String html=OpportunityTestData.html("Synthetic Company","Internship","R1");
        assertEquals(Eligibility.UNKNOWN,extract(html).candidates().getFirst().input().mauritianEligibility());
        assertEquals(Eligibility.UNKNOWN,extract(html).candidates().getFirst().input().internationalEligibility());
    }
}
