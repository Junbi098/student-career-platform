package com.studentcareer.company.persistence;

import com.studentcareer.company.domain.Company;
import com.studentcareer.company.domain.CompanyDossier;
import com.studentcareer.company.importing.ImportStatus;
import com.studentcareer.company.importing.SourceImportRequest;
import com.studentcareer.company.importing.SourceImportResult;
import com.studentcareer.company.repository.CompanyRepository;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.model.SourceReference;
import com.studentcareer.source.model.SourceVerificationStatus;
import com.studentcareer.support.TestDatabaseCleanup;
import com.studentcareer.support.TestDatabaseSafetyInitializer;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@ContextConfiguration(initializers = TestDatabaseSafetyInitializer.class)
class JpaCompanyRepositoryIntegrationTest {
    @Autowired CompanyRepository repository;
    @Autowired JdbcTemplate jdbc;
    private final Instant accessed = Instant.parse("2026-11-05T12:00:00.123456Z");
    private final Instant updated = Instant.parse("2020-01-01T00:00:00Z");

    @BeforeEach
    void clearIsolatedDatabase() {
        TestDatabaseCleanup.clear(jdbc);
    }

    private SourceImportResult item(String id, String name) {
        URI uri = URI.create("https://example.com/" + id);
        var source = new SourceReference(uri, "Synthetic source", accessed, SourceKind.WEB_OBSERVATION, updated, "CC0-1.0");
        var company = new Company(name, "https://example.com", null, "Mauritius");
        return new SourceImportResult(new SourceImportRequest(uri, SourceKind.WEB_OBSERVATION),
                ImportStatus.SUCCESS, new CompanyDossier(company, source), null, null);
    }

    @Test
    void shouldCommitDossierAndAllProvenanceToRealPostgreSql() {
        repository.saveAll(List.of(item("a", "Synthetic Company")));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM company_dossiers", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM source_references", Integer.class));
        var loaded = repository.findAll().getFirst();
        assertEquals("Synthetic Company", loaded.dossier().getCompany().getName());
        assertNull(loaded.dossier().getCompany().getIndustry());
        var source = loaded.dossier().getSource();
        assertEquals(accessed, source.getAccessedAt());
        assertEquals(updated, source.getSourceUpdatedAt());
        assertEquals("CC0-1.0", source.getLicense());
        assertEquals(SourceVerificationStatus.UNVERIFIED, source.getVerificationStatus());
        assertNull(jdbc.queryForObject("SELECT verified_at FROM source_references", Object.class));
        assertEquals(source.getUrl(), repository.findById(loaded.id()).orElseThrow().dossier().getSource().getUrl());
    }

    @Test
    void shouldReimportWithoutAddingRowsOrChangingIdsAndOrder() {
        repository.saveAll(List.of(item("a", "First"), item("b", "Second")));
        var ids = repository.findAll().stream().map(entry -> entry.id()).toList();
        repository.saveAll(List.of(item("a", "Updated"), item("b", "Second")));
        assertEquals(ids, repository.findAll().stream().map(entry -> entry.id()).toList());
        assertEquals("Updated", repository.findAll().getFirst().dossier().getCompany().getName());
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM source_references", Integer.class));
        var renewal = item("a", "Updated");
        var previous = renewal.dossier().getSource();
        var refreshed = new SourceReference(previous.getUrl(), previous.getTitle(), accessed.plusSeconds(60),
                previous.getKind(), updated, previous.getLicense());
        repository.saveAll(List.of(new SourceImportResult(renewal.request(), ImportStatus.SUCCESS,
                new CompanyDossier(renewal.dossier().getCompany(), refreshed), null, null)));
        assertEquals(accessed.plusSeconds(60), repository.findAll().getFirst().dossier().getSource().getAccessedAt());
    }

    @Test
    void shouldKeepPossibleDuplicateAndItsIndependentSource() {
        var original = item("a", "Same Company");
        var second = item("b", "Same Company");
        var duplicate = new SourceImportResult(second.request(), ImportStatus.POSSIBLE_DUPLICATE,
                second.dossier(), original.request().uri(), null);
        repository.saveAll(List.of(original, duplicate));
        assertEquals(2, repository.findAll().size());
        assertEquals(ImportStatus.POSSIBLE_DUPLICATE, repository.findAll().get(1).importStatus());
        assertEquals(original.request().uri(), repository.findAll().get(1).duplicateOf());
    }

    @Test
    void shouldRollBackWholeWriteIfOneDossierViolatesSchema() {
        assertThrows(DataIntegrityViolationException.class,
                () -> repository.saveAll(List.of(item("a", "Valid"), item("b", "X".repeat(513)))));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM company_dossiers", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM source_references", Integer.class));
    }

    @Test
    void shouldApplyFlywayAndUseNonSuperuserAccount() {
        assertEquals(Boolean.TRUE, jdbc.queryForObject(
                "SELECT success FROM flyway_schema_history WHERE version = '1'", Boolean.class));
        assertEquals(Boolean.FALSE, jdbc.queryForObject(
                "SELECT rolsuper FROM pg_roles WHERE rolname = current_user", Boolean.class));
        repository.saveAll(List.of(item("a", "Synthetic")));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "UPDATE source_references SET verification_status = 'FICTIONAL'"));
    }

    @Test
    void shouldSerializeConcurrentReimportsWithinOneApplication() throws Exception {
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> repository.saveAll(List.of(item("a", "First"))));
            var second = executor.submit(() -> repository.saveAll(List.of(item("a", "First"))));
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
        }
        assertEquals(1, repository.findAll().size());
    }
}
