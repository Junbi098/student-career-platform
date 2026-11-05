package com.studentcareer.company.importing;

import com.studentcareer.company.exception.CollectionRateLimitException;
import com.studentcareer.company.repository.InMemoryCompanyRepository;
import com.studentcareer.source.model.SourceDocument;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.model.SourceReference;
import com.studentcareer.source.model.SourceVerificationStatus;
import com.studentcareer.source.reader.SourceReader;
import com.studentcareer.support.WikidataTestData;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WikidataCompanyImportServiceTest {
    private final InMemoryCompanyRepository repository = new InMemoryCompanyRepository();
    private final MutableClock clock = new MutableClock();

    private SourceReader reader(boolean failSecond) {
        return (uri, kind) -> {
            if (failSecond && uri.toString().contains("Q3053417")) {
                throw new IOException("Simulated source outage");
            }
            String id = uri.getPath().substring(uri.getPath().lastIndexOf('/') + 1).replace(".json", "");
            var document = WikidataTestData.document(id);
            ((com.fasterxml.jackson.databind.node.ObjectNode) document.path("entities").path(id).path("labels").path("en"))
                    .put("value", "Synthetic Company " + id);
            return new SourceDocument(document.toString(), new SourceReference(uri, "Simulated Wikidata source",
                    clock.instant(), SourceKind.WEB_OBSERVATION, Instant.EPOCH, "CC0-1.0"));
        };
    }

    @Test
    void shouldKeepRealSourceCategoryProvenanceAndStableIdentifiersOnReimport() {
        var service = new WikidataCompanyImportService(repository, reader(false), clock);
        var result = service.importCompanies();
        assertEquals(3, result.count(ImportStatus.SUCCESS));
        assertEquals(3, repository.findAll().size());
        var first = repository.findAll().getFirst();
        assertEquals(SourceKind.WEB_OBSERVATION, first.dossier().getSource().getKind());
        assertEquals(SourceVerificationStatus.UNVERIFIED, first.dossier().getSource().getVerificationStatus());
        clock.now = clock.now.plusSeconds(61);
        service.importCompanies();
        assertEquals(3, repository.findAll().size());
        assertEquals(first.id(), repository.findAll().getFirst().id());
        assertEquals(clock.instant(), repository.findAll().getFirst().dossier().getSource().getAccessedAt());
    }

    @Test
    void shouldContinueAfterOneSourceFailsAndNotInventDossier() {
        var service = new WikidataCompanyImportService(repository, reader(true), clock);
        var result = service.importCompanies();
        assertEquals(2, result.count(ImportStatus.SUCCESS));
        assertEquals(1, result.count(ImportStatus.READ_ERROR));
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void shouldRefuseImmediateRepeatedImportEvenAfterOutage() {
        SourceReader unavailable = (uri, kind) -> { throw new IOException("Simulated outage"); };
        var service = new WikidataCompanyImportService(repository, unavailable, clock);
        assertEquals(3, service.importCompanies().count(ImportStatus.READ_ERROR));
        var exception = assertThrows(CollectionRateLimitException.class, service::importCompanies);
        assertTrue(exception.getRetryAfter() >= 60);
        assertTrue(repository.findAll().isEmpty());
    }

    private static class MutableClock extends Clock {
        Instant now = Instant.parse("2026-11-05T00:00:00Z");
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }
}
