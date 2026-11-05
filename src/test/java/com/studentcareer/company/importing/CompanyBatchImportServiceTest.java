package com.studentcareer.company.importing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.company.domain.Company;
import com.studentcareer.company.export.CompanyBatchJsonWriter;
import com.studentcareer.company.parsing.CompanyHtmlParser;
import com.studentcareer.company.service.CompanyAnalysisService;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.reader.LocalFileSourceReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class CompanyBatchImportServiceTest {
    @TempDir Path directory;
    private final CompanyBatchImportService batch = new CompanyBatchImportService(
            new CompanyAnalysisService(new LocalFileSourceReader(), new CompanyHtmlParser()));

    private SourceImportRequest company(String file, String name, String website) throws Exception {
        String html = "<h1 class='company-name'>" + name + "</h1>"
                + (website == null ? "" : "<a class='company-website' href='" + website + "'>Site</a>");
        return new SourceImportRequest(Files.writeString(directory.resolve(file), html).toUri(), SourceKind.FIXTURE);
    }

    @Test
    void shouldProcessMixedInputsInOrderAndContinueAfterErrors() throws Exception {
        var first = company("first.html", "Island Studio", "https://example.com");
        var missing = new SourceImportRequest(directory.resolve("missing.html").toUri(), SourceKind.FIXTURE);
        var web = new SourceImportRequest(URI.create("https://example.com/not-read"), SourceKind.LOCAL_DOCUMENT);
        var empty = new SourceImportRequest(Files.writeString(directory.resolve("empty.html"), "<p>No name</p>").toUri(),
                SourceKind.FIXTURE);
        var duplicate = company("duplicate.html", " island   studio ", "https://EXAMPLE.com:443/");
        var other = company("other.html", "Another Company", "https://example.org");
        var inputs = List.of(first, missing, web, empty, duplicate, other);
        BatchImportResult result = batch.analyze(inputs);
        assertEquals(inputs, result.items().stream().map(SourceImportResult::request).toList());
        assertEquals(List.of(ImportStatus.SUCCESS, ImportStatus.READ_ERROR, ImportStatus.INVALID_SOURCE,
                ImportStatus.NO_COMPANY, ImportStatus.POSSIBLE_DUPLICATE, ImportStatus.SUCCESS),
                result.items().stream().map(SourceImportResult::status).toList());
        assertEquals(2, result.count(ImportStatus.SUCCESS));
        assertEquals(Path.of(first.uri()).toRealPath().toUri(), result.items().get(4).duplicateOf());
        assertEquals(Path.of(duplicate.uri()).toRealPath().toUri(), result.items().get(4).dossier().getSource().getUrl());
        assertNotNull(result.items().get(4).dossier());
        assertNull(result.items().get(1).dossier());
        assertNotNull(result.items().get(1).message());
    }

    @Test
    void shouldNeverUseNameAloneToIdentifyDuplicates() throws Exception {
        var result = batch.analyze(List.of(company("a.html", "Island", null), company("b.html", "Island", null)));
        assertEquals(2, result.count(ImportStatus.SUCCESS));
    }

    @Test
    void shouldKeepDifferentWebsitePathsAndCompaniesSeparate() throws Exception {
        var result = batch.analyze(List.of(
                company("a.html", "Island", "https://example.com/Team"),
                company("b.html", "Island", "https://example.com/team"),
                company("c.html", "Other", "https://example.com/Team")));
        assertEquals(3, result.count(ImportStatus.SUCCESS));
    }

    @Test
    void shouldNotGroupFixturesWithLocalDocuments() throws Exception {
        var fixture = company("a.html", "Island", "https://example.com");
        var local = new SourceImportRequest(fixture.uri(), SourceKind.LOCAL_DOCUMENT);
        assertEquals(2, batch.analyze(List.of(fixture, local)).count(ImportStatus.SUCCESS));
    }

    @Test
    void shouldRestartDuplicateDetectionForEachBatch() throws Exception {
        var request = company("a.html", "Island", "https://example.com");
        batch.analyze(List.of(request));
        assertEquals(ImportStatus.SUCCESS, batch.analyze(List.of(request)).items().getFirst().status());
    }

    @Test
    void shouldHandleEmptyBatchAndExposeImmutableResults() {
        BatchImportResult result = batch.analyze(List.of());
        assertTrue(result.items().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> result.items().add(null));
    }

    @Test
    void shouldRejectNullRequestsBeforeReading() {
        assertThrows(NullPointerException.class, () -> batch.analyze(null));
        assertThrows(NullPointerException.class, () -> new SourceImportRequest(null, SourceKind.FIXTURE));
    }

    @Test
    void shouldExportSummaryAndRetainBothDossiers() throws Exception {
        var first = company("a.html", "Island", "https://example.com");
        var second = company("b.html", "Island", "https://example.com");
        var missing = new SourceImportRequest(directory.resolve("missing.html").toUri(), SourceKind.FIXTURE);
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
        assertEquals(2, result.count(ImportStatus.SUCCESS));
    }

    @Test
    void shouldKeepAllRepeatedInputsAndTheirProvenance() throws Exception {
        var first = company("a.html", "Island", "https://example.com");
        var second = company("b.html", "Island", "https://example.com");
        var third = company("c.html", "Island", "https://example.com");
        var result = batch.analyze(List.of(first, second, third));
        assertEquals(1, result.count(ImportStatus.SUCCESS));
        assertEquals(2, result.count(ImportStatus.POSSIBLE_DUPLICATE));
        assertEquals(result.items().get(1).duplicateOf(), result.items().get(2).duplicateOf());
        assertEquals(3, result.items().stream().map(item -> item.dossier().getSource().getUrl()).distinct().count());
    }

    @Test
    void shouldReportDirectoryReadErrorAndContinue() throws Exception {
        var folder = new SourceImportRequest(directory.toUri(), SourceKind.LOCAL_DOCUMENT);
        var valid = company("valid.html", "Island", null);
        var result = batch.analyze(List.of(folder, valid));
        assertEquals(ImportStatus.READ_ERROR, result.items().getFirst().status());
        assertEquals(ImportStatus.SUCCESS, result.items().get(1).status());
    }

}
