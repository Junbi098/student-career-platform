package com.studentcareer.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.company.domain.*;
import com.studentcareer.company.importing.*;
import com.studentcareer.company.repository.CompanyRepository;
import com.studentcareer.source.model.*;
import com.studentcareer.support.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ContextConfiguration(initializers=TestDatabaseSafetyInitializer.class)
class OpportunityApiIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired CompanyRepository companies;
    private final ObjectMapper mapper=new ObjectMapper();
    private final HttpClient client=HttpClient.newHttpClient();
    private UUID company;

    @BeforeEach void seedSyntheticCompanyOnlyInTestDatabase() {
        TestDatabaseCleanup.clear(jdbc);
        URI uri=URI.create("classpath:/test/synthetic-opportunity-company.html");
        var source=new SourceReference(uri,"Synthetic company",Instant.EPOCH,SourceKind.FIXTURE);
        companies.saveAll(List.of(new SourceImportResult(new SourceImportRequest(uri,SourceKind.FIXTURE),ImportStatus.SUCCESS,
                new CompanyDossier(new Company("Synthetic Company",null,null,"Mauritius"),source),null,null)));
        company=companies.findAll().getFirst().id();
    }
    private Map<String,Object> body() {
        Map<String,Object> data=new LinkedHashMap<>();
        data.put("companyId",company.toString());data.put("title","Synthetic Java internship");data.put("type","INTERNSHIP");
        data.put("description","Synthetic Java role exclusively for isolated tests.");data.put("sector","Software");
        data.put("location","Ébène, Mauritius");data.put("workMode","HYBRID");data.put("deadline","2099-12-31");
        data.put("sourceUrl","https://example.test/jobs/REF1");data.put("sourceTitle","Synthetic posting");data.put("externalReference","REF1");
        return data;
    }
    private HttpResponse<String> send(String method,String path,Object body) throws Exception {
        var builder=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(Duration.ofSeconds(10));
        if(body==null)builder.method(method,HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type","application/json").method(method,HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    private JsonNode ok(HttpResponse<String> response,int status) throws Exception {
        assertEquals(status,response.statusCode(),response.body()); return mapper.readTree(response.body());
    }
    @Test void shouldCreateConfirmSearchUpdateAndKeepReviewHistory() throws Exception {
        var data=body(); var created=ok(send("POST","/api/opportunities",data),201);
        String id=created.path("id").asText();
        assertEquals("UNVERIFIED",created.path("status").asText());assertFalse(created.path("sourceFetchedByServer").asBoolean());
        assertTrue(created.path("sourceAccessedAt").isNull());
        assertEquals(409,send("POST","/api/opportunities",data).statusCode());
        var before=ok(send("GET","/api/opportunities?verifiedOnly=true",null),200);
        assertEquals(0,before.path("totalElements").asInt());
        var confirmed=ok(send("POST","/api/opportunities/"+id+"/review",Map.of("version",created.path("version").asLong(),
                "action","CONFIRM","sourceReviewed",true,"evidenceUrl",data.get("sourceUrl"),"notes","Synthetic operator review in test database")),200);
        assertEquals("OPEN",confirmed.path("status").asText());
        var page=ok(send("GET","/api/opportunities?q=java&location=ebene&type=INTERNSHIP&verifiedOnly=true",null),200);
        assertEquals(1,page.path("totalElements").asInt());
        assertEquals(409,send("PUT","/api/opportunities/"+id,Map.of("version",created.path("version").asLong(),"opportunity",data)).statusCode());
        data.put("title","Synthetic Java internship updated");
        var changed=ok(send("PUT","/api/opportunities/"+id,Map.of("version",confirmed.path("version").asLong(),"opportunity",data)),200);
        assertEquals("UNVERIFIED",changed.path("status").asText());assertTrue(changed.path("lastVerifiedAt").isNull());
        assertEquals(3,ok(send("GET","/api/opportunities/"+id+"/reviews",null),200).size());
    }
    @Test void shouldDetectHtmlWithoutClaimingAnOutboundFetchAndDeduplicate() throws Exception {
        var input=Map.of("companyId",company.toString(),"html",OpportunityTestData.html("Synthetic Company","Java internship","HTML1"),
                "sourceUrl","https://example.test/jobs","sourceTitle","Synthetic submitted page");
        var first=ok(send("POST","/api/opportunities/imports/html",input),200);
        assertEquals(1,first.path("created").asInt());
        assertEquals(1,ok(send("POST","/api/opportunities/imports/html",input),200).path("unchanged").asInt());
        String id=first.path("results").get(0).path("id").asText();
        var detail=ok(send("GET","/api/opportunities/"+id,null),200);
        assertEquals("HTML_SUBMISSION",detail.path("origin").asText());
        assertFalse(detail.path("sourceFetchedByServer").asBoolean());
        assertEquals("UNKNOWN",detail.path("details").path("internationalEligibility").asText());
    }
    @Test void shouldRejectInvalidDatesEligibilityVersionsAndMissingRecords() throws Exception {
        var data=body(); data.put("deadline","invalid-date");
        assertEquals(400,send("POST","/api/opportunities",data).statusCode());
        data=body();data.put("internationalEligibility","YES");
        assertEquals(400,send("POST","/api/opportunities",data).statusCode());
        data=body();data.put("companyId",UUID.randomUUID().toString());
        assertEquals(404,send("POST","/api/opportunities",data).statusCode());
        assertEquals(404,send("GET","/api/opportunities/"+UUID.randomUUID(),null).statusCode());
        assertEquals(400,send("GET","/api/opportunities?size=101",null).statusCode());
        assertEquals(400,send("GET","/api/opportunities?type=SENIOR_JOB",null).statusCode());
        assertEquals(400,send("GET","/api/opportunities?page=-1",null).statusCode());
    }
    @Test void shouldCloseAndReopenWithoutIrrelevantSourceReviewFields() throws Exception {
        var created=ok(send("POST","/api/opportunities",body()),201);
        String id=created.path("id").asText();
        var closed=ok(send("POST","/api/opportunities/"+id+"/review",Map.of(
                "version",created.path("version").asLong(),"action","CLOSE","notes","Synthetic closure for API test")),200);
        assertEquals("CLOSED",closed.path("status").asText());
        var reopened=ok(send("POST","/api/opportunities/"+id+"/review",Map.of(
                "version",closed.path("version").asLong(),"action","REOPEN","notes","Synthetic reopening for API test")),200);
        assertEquals("UNVERIFIED",reopened.path("status").asText());
    }
}
