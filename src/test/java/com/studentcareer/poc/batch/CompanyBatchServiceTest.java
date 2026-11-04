package com.studentcareer.poc.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.poc.CompanyHtmlParser;
import com.studentcareer.poc.domain.model.SourceKind;
import com.studentcareer.poc.fetch.LocalFileSourceFetcher;
import com.studentcareer.poc.output.CompanyBatchJsonWriter;
import com.studentcareer.poc.service.CompanyIntelligenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CompanyBatchServiceTest {
    @TempDir Path directory;
    private final CompanyBatchService batch = new CompanyBatchService(
            new CompanyIntelligenceService(new LocalFileSourceFetcher(), new CompanyHtmlParser()));

    private SourceRequest company(String file, String name, String website) throws Exception {
        String html = "<h1 class='company-name'>" + name + "</h1>"
                + (website == null ? "" : "<a class='company-website' href='" + website + "'>Site</a>");
        return new SourceRequest(Files.writeString(directory.resolve(file), html).toUri(), SourceKind.FIXTURE);
    }

    @Test
    void shouldProcessMixedInputsInOrderAndContinueAfterErrors() throws Exception {
        var first = company("first.html", "Island Studio", "https://example.com");
        var missing = new SourceRequest(directory.resolve("missing.html").toUri(), SourceKind.FIXTURE);
        var web = new SourceRequest(URI.create("https://example.com/not-read"), SourceKind.LOCAL_DOCUMENT);
        var empty = new SourceRequest(Files.writeString(directory.resolve("empty.html"), "<p>No name</p>").toUri(),
                SourceKind.FIXTURE);
        var duplicate = company("duplicate.html", " island   studio ", "https://EXAMPLE.com:443/");
        var other = company("other.html", "Another Company", "https://example.org");
        var inputs = List.of(first, missing, web, empty, duplicate, other);
        BatchResult result = batch.analyze(inputs);
        assertEquals(inputs, result.items().stream().map(BatchItem::request).toList());
        assertEquals(List.of(BatchStatus.SUCCESS, BatchStatus.READ_ERROR, BatchStatus.INVALID_SOURCE,
                BatchStatus.NO_COMPANY, BatchStatus.POSSIBLE_DUPLICATE, BatchStatus.SUCCESS),
                result.items().stream().map(BatchItem::status).toList());
        assertEquals(2, result.count(BatchStatus.SUCCESS));
        assertEquals(Path.of(first.uri()).toRealPath().toUri(), result.items().get(4).duplicateOf());
        assertEquals(Path.of(duplicate.uri()).toRealPath().toUri(), result.items().get(4).dossier().getSource().getUrl());
        assertNotNull(result.items().get(4).dossier());
        assertNull(result.items().get(1).dossier());
        assertNotNull(result.items().get(1).message());
    }

    @Test
    void shouldNeverUseNameAloneToIdentifyDuplicates() throws Exception {
        var result = batch.analyze(List.of(company("a.html", "Island", null), company("b.html", "Island", null)));
        assertEquals(2, result.count(BatchStatus.SUCCESS));
    }

    @Test
    void shouldKeepDifferentWebsitePathsAndCompaniesSeparate() throws Exception {
        var result = batch.analyze(List.of(
                company("a.html", "Island", "https://example.com/Team"),
                company("b.html", "Island", "https://example.com/team"),
                company("c.html", "Other", "https://example.com/Team")));
        assertEquals(3, result.count(BatchStatus.SUCCESS));
    }

    @Test
    void shouldNotGroupFixturesWithLocalDocuments() throws Exception {
        var fixture = company("a.html", "Island", "https://example.com");
        var local = new SourceRequest(fixture.uri(), SourceKind.LOCAL_DOCUMENT);
        assertEquals(2, batch.analyze(List.of(fixture, local)).count(BatchStatus.SUCCESS));
    }

    @Test
    void shouldRestartDuplicateDetectionForEachBatch() throws Exception {
        var request = company("a.html", "Island", "https://example.com");
        batch.analyze(List.of(request));
        assertEquals(BatchStatus.SUCCESS, batch.analyze(List.of(request)).items().getFirst().status());
    }

    @Test
    void shouldHandleEmptyBatchAndExposeImmutableResults() {
        BatchResult result = batch.analyze(List.of());
        assertTrue(result.items().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.items().add(null));
    }

    @Test
    void shouldRejectNullRequestsBeforeReading() {
        assertThrows(NullPointerException.class, () -> batch.analyze(null));
        assertThrows(NullPointerException.class, () -> new SourceRequest(null, SourceKind.FIXTURE));
    }

    @Test
    void shouldExportSummaryAndRetainBothDossiers() throws Exception {
        var first = company("a.html", "Island", "https://example.com");
        var second = company("b.html", "Island", "https://example.com");
        var missing = new SourceRequest(directory.resolve("missing.html").toUri(), SourceKind.FIXTURE);
        String json = new CompanyBatchJsonWriter().write(batch.analyze(List.of(first, second, missing)));
        var root = new ObjectMapper().readTree(json);
        assertEquals(3, root.path("summary").path("total").asInt());
        assertEquals(1, root.path("summary").path("POSSIBLE_DUPLICATE").asInt());
        assertEquals(1, root.path("summary").path("READ_ERROR").asInt());
        assertEquals("FIXTURE", root.path("results").get(1).path("dossier").path("source").path("kind").asText());
        assertEquals(Path.of(first.uri()).toRealPath().toUri().toString(), root.path("results").get(1).path("duplicateOf").asText());
        assertEquals(Path.of(second.uri()).toRealPath().toUri().toString(), root.path("results").get(1).path("dossier").path("source").path("url").asText());
        assertTrue(root.path("results").get(2).path("dossier").isNull());
    }
    @Test
    void shouldKeepDifferentQueriesSeparate() throws Exception {
        var result = batch.analyze(List.of(
                company("a.html", "Island", "https://example.com/?company=one"),
                company("b.html", "Island", "https://example.com/?company=two")));
        assertEquals(2, result.count(BatchStatus.SUCCESS));
    }

    @Test
    void shouldKeepAllRepeatedInputsAndTheirProvenance() throws Exception {
        var first = company("a.html", "Island", "https://example.com");
        var second = company("b.html", "Island", "https://example.com");
        var third = company("c.html", "Island", "https://example.com");
        var result = batch.analyze(List.of(first, second, third));
        assertEquals(1, result.count(BatchStatus.SUCCESS));
        assertEquals(2, result.count(BatchStatus.POSSIBLE_DUPLICATE));
        assertEquals(result.items().get(1).duplicateOf(), result.items().get(2).duplicateOf());
        assertEquals(3, result.items().stream().map(item -> item.dossier().getSource().getUrl()).distinct().count());
    }

    @Test
    void shouldReportDirectoryReadErrorAndContinue() throws Exception {
        var folder = new SourceRequest(directory.toUri(), SourceKind.LOCAL_DOCUMENT);
        var valid = company("valid.html", "Island", null);
        var result = batch.analyze(List.of(folder, valid));
        assertEquals(BatchStatus.READ_ERROR, result.items().getFirst().status());
        assertEquals(BatchStatus.SUCCESS, result.items().get(1).status());
    }

}
