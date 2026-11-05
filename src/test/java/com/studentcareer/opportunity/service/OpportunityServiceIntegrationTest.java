package com.studentcareer.opportunity.service;

import com.studentcareer.company.domain.*;
import com.studentcareer.company.importing.*;
import com.studentcareer.company.repository.CompanyRepository;
import com.studentcareer.opportunity.model.*;
import com.studentcareer.opportunity.model.OpportunityTypes.*;
import com.studentcareer.opportunity.service.OpportunityContracts.*;
import com.studentcareer.source.model.*;
import com.studentcareer.support.*;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers=TestDatabaseSafetyInitializer.class)
class OpportunityServiceIntegrationTest {
    @Autowired OpportunityService service;
    @Autowired JobPostingImportService imports;
    @Autowired CompanyRepository companies;
    @Autowired JdbcTemplate jdbc;
    private UUID company;

    @BeforeEach void prepare() {
        TestDatabaseCleanup.clear(jdbc);
        URI uri=URI.create("classpath:/test/synthetic-company.html");
        var source=new SourceReference(uri,"Synthetic company fixture",Instant.EPOCH,SourceKind.FIXTURE);
        companies.saveAll(List.of(new SourceImportResult(new SourceImportRequest(uri,SourceKind.FIXTURE),ImportStatus.SUCCESS,
                new CompanyDossier(new Company("Synthetic Company",null,"Software","Mauritius"),source),null,null)));
        company=companies.findAll().getFirst().id();
    }
    private OpportunityInput input(String ref,String title,Eligibility policy) {
        return OpportunityTestData.input(company,ref,title,policy);
    }
    private Query query(StudentType student,boolean includeUnknown,Status status,boolean verified,boolean inactive,String q) {
        return new Query(q,null,null,null,null,student,includeUnknown,status,verified,inactive,0,20,Sort.RELEVANCE);
    }
    private View confirm(View view) {
        return service.review(view.id(),new Review(view.version(),ReviewAction.CONFIRM,true,view.details().sourceUrl(),
                "Synthetic source checked in isolated automated test"));
    }
    @Test void shouldPersistUnverifiedSubmissionAndDeclareSourceWasNotFetched() {
        View created=service.create(input("a","Java internship",Eligibility.UNKNOWN));
        var found=service.find(created.id());
        assertEquals(Status.UNVERIFIED,found.status()); assertFalse(found.sourceFetchedByServer());
        assertNull(found.sourceAccessedAt()); assertNull(found.lastVerifiedAt());
        assertEquals(1,service.history(found.id()).size());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM opportunities",Integer.class));
    }
    @Test void shouldRejectDuplicateAndLeaveSingleRow() {
        var input=input("a","Java internship",Eligibility.UNKNOWN);
        service.create(input);
        assertThrows(OpportunityConflictException.class,()->service.create(input));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM opportunities",Integer.class));
    }
    @Test void shouldRequireAcknowledgedMatchingSourceForConfirmation() {
        View created=service.create(input("a","Java internship",Eligibility.UNKNOWN));
        assertThrows(com.studentcareer.common.exception.InvalidRequestException.class,()->service.review(created.id(),
                new Review(created.version(),ReviewAction.CONFIRM,false,created.details().sourceUrl(),"Synthetic review notes")));
        assertThrows(com.studentcareer.common.exception.InvalidRequestException.class,()->service.review(created.id(),
                new Review(created.version(),ReviewAction.CONFIRM,true,"https://other.test", "Synthetic review notes")));
        View confirmed=confirm(created);
        assertEquals(Status.OPEN,confirmed.status()); assertNotNull(confirmed.lastVerifiedAt());
        assertEquals(2,service.history(created.id()).size());
    }
    @Test void shouldInvalidateConfirmationAfterMaterialUpdateAndRejectStaleVersion() {
        var original=input("a","Java internship",Eligibility.UNKNOWN);
        View created=service.create(original); View confirmed=confirm(created);
        View updated=service.update(created.id(),new Update(confirmed.version(),OpportunityTestData.title(original,"Updated Java internship")));
        assertEquals(Status.UNVERIFIED,updated.status()); assertNull(updated.lastVerifiedAt());
        assertEquals(3,service.history(created.id()).size());
        assertThrows(OpportunityConflictException.class,()->service.update(created.id(),new Update(created.version(),original)));
    }
    @Test void shouldCloseReopenAndRejectWithoutLosingAuditHistory() {
        View view=confirm(service.create(input("a","Java internship",Eligibility.UNKNOWN)));
        view=service.review(view.id(),new Review(view.version(),ReviewAction.CLOSE,false,null,"Synthetic employer closure"));
        assertEquals(Status.CLOSED,view.status());
        assertEquals(0,service.search(query(null,false,null,false,false,null)).totalElements());
        view=service.review(view.id(),new Review(view.version(),ReviewAction.REOPEN,false,null,"Synthetic opportunity reopened"));
        assertEquals(Status.UNVERIFIED,view.status());
        view=service.review(view.id(),new Review(view.version(),ReviewAction.REJECT,true,view.details().sourceUrl(),"Synthetic invalid posting rejected"));
        assertEquals(Status.REJECTED,view.status());
        assertEquals(0,service.search(query(null,false,null,false,false,null)).totalElements());
        assertEquals(1,service.search(query(null,false,Status.REJECTED,false,true,null)).totalElements());
        assertEquals(5,service.history(view.id()).size());
    }
    @Test void shouldExcludeExpiredByDefaultAndRefuseConfirmation() {
        var input=input("a","Old internship",Eligibility.UNKNOWN);
        var expired=new OpportunityInput(company,input.title(),input.type(),input.description(),input.sector(),input.location(),input.workMode(),
                input.publishedOn(),"2020-01-02",input.sourceUrl(),input.sourceTitle(),input.externalReference(),input.applicationUrl(),
                input.mauritianEligibility(),input.internationalEligibility(),input.eligibilityDetails());
        View view=service.create(expired); assertEquals(Status.EXPIRED,view.status());
        assertEquals(0,service.search(query(null,false,null,false,false,null)).totalElements());
        assertEquals(1,service.search(query(null,false,Status.EXPIRED,false,true,null)).totalElements());
        assertThrows(OpportunityConflictException.class,()->confirm(view));
    }
    @Test void shouldTreatOldConfirmationAsStaleWithoutScheduledTask() {
        View view=confirm(service.create(input("a","Java internship",Eligibility.UNKNOWN)));
        jdbc.update("UPDATE opportunities SET last_verified_at = ? WHERE id = ?",
                java.sql.Timestamp.from(Instant.now().minusSeconds(31L*86400)),view.id());
        assertEquals(Status.STALE,service.find(view.id()).status());
        assertEquals(0,service.search(query(null,false,null,true,false,null)).totalElements());
    }
    @Test void shouldFilterEligibilityWithoutTreatingUnknownAsAllowed() {
        service.create(input("yes","Java internship",Eligibility.YES));
        service.create(input("unknown","Java placement",Eligibility.UNKNOWN));
        service.create(input("no","Java training",Eligibility.NO));
        assertEquals(1,service.search(query(StudentType.INTERNATIONAL,false,null,false,false,null)).totalElements());
        assertEquals(2,service.search(query(StudentType.INTERNATIONAL,true,null,false,false,null)).totalElements());
        var result=service.search(new Query("java","software","ebene",Type.INTERNSHIP,WorkMode.HYBRID,
                StudentType.INTERNATIONAL,true,null,false,false,0,1,Sort.RELEVANCE));
        assertEquals(2,result.totalElements());assertEquals(2,result.totalPages());
        assertEquals(Eligibility.YES,result.items().getFirst().selectedStudentEligibility());
        assertTrue(result.items().getFirst().matchedCriteria().contains("DECLARED_ELIGIBILITY"));
        assertTrue(service.search(new Query(null,null,null,null,null,null,false,null,false,false,Integer.MAX_VALUE,100,Sort.NEWEST)).items().isEmpty());
    }
    @Test void shouldImportDeduplicateAndInvalidateChangedStructuredPosting() {
        var request=new HtmlImport(company,OpportunityTestData.html("Synthetic Company","Java internship","REF1"),
                "https://example.test/jobs","Synthetic submitted HTML");
        var first=imports.ingest(request);assertEquals(1,first.created());
        UUID id=first.results().getFirst().id();
        assertEquals(1,imports.ingest(request).unchanged());
        confirm(service.find(id));
        var changed=new HtmlImport(company,request.html().replace("Java internship","Java internship updated"),request.sourceUrl(),request.sourceTitle());
        assertEquals(1,imports.ingest(changed).updated());
        assertEquals(id,imports.ingest(changed).results().getFirst().id());
        assertEquals(Status.UNVERIFIED,service.find(id).status());
        assertEquals(Origin.HTML_SUBMISSION,service.find(id).origin());
        assertFalse(service.find(id).sourceFetchedByServer());
    }
    @Test void shouldNeverTurnCompanyExistenceIntoOpportunity() {
        assertEquals(1,companies.findAll().size());
        assertEquals(0,service.search(query(null,false,null,false,false,null)).totalElements());
    }
    @Test void shouldKeepReviewDateWhenIdenticalHtmlIsResubmitted() {
        var request=new HtmlImport(company,OpportunityTestData.html("Synthetic Company","Java internship","SAME"),
                "https://example.test/job","Synthetic supplied HTML");
        var imported=imports.ingest(request);
        var confirmed=confirm(service.find(imported.results().getFirst().id()));
        assertEquals(1,imports.ingest(request).unchanged());
        assertEquals(confirmed.lastVerifiedAt(),service.find(confirmed.id()).lastVerifiedAt());
        assertEquals(Status.OPEN,service.find(confirmed.id()).status());
    }
    @Test void shouldReportRejectedEmployerAndContinueValidPosting() {
        var request=new HtmlImport(company,OpportunityTestData.html("Other Company","Internship","WRONG")+
                OpportunityTestData.html("Synthetic Company","Internship","RIGHT"),"https://example.test/jobs","Synthetic HTML");
        var report=imports.ingest(request);
        assertEquals(2,report.detected()); assertEquals(1,report.created()); assertEquals(1,report.rejected());
    }
}
