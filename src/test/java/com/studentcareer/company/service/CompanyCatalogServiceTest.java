package com.studentcareer.company.service;

import com.studentcareer.common.exception.InvalidRequestException;
import com.studentcareer.company.domain.Company;
import com.studentcareer.company.domain.CompanyDossier;
import com.studentcareer.company.exception.CompanyNotFoundException;
import com.studentcareer.company.importing.ImportStatus;
import com.studentcareer.company.importing.SourceImportRequest;
import com.studentcareer.company.importing.SourceImportResult;
import com.studentcareer.company.repository.InMemoryCompanyRepository;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.model.SourceReference;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompanyCatalogServiceTest {
    private final InMemoryCompanyRepository catalog = new InMemoryCompanyRepository();
    private final CompanyCatalogService service = new CompanyCatalogService(catalog);

    private SourceImportResult item(String resource, String name, String industry, String location) {
        URI uri = URI.create("classpath:/demo/" + resource);
        var source = new SourceReference(uri, "Fictional fixture", Instant.EPOCH, SourceKind.FIXTURE);
        var dossier = new CompanyDossier(new Company(name, null, industry, location), source);
        return new SourceImportResult(new SourceImportRequest(uri, SourceKind.FIXTURE), ImportStatus.SUCCESS, dossier, null, null);
    }

    @Test
    void shouldStartEmpty() {
        var result = service.search(null, null, null, 0, 20);
        assertEquals(0, result.totalElements());
        assertEquals(0, result.totalPages());
        assertTrue(result.items().isEmpty());
    }

    @Test
    void shouldCombineCaseInsensitiveFiltersAndPreserveSources() {
        catalog.saveAll(List.of(item("a", "Island Digital Studio", "Software Development", "Ebene, Mauritius"),
                item("b", "Lagoon Learning Lab", "Education", "Port Louis")));
        var result = service.search("  ISLAND  ", "SOFTWARE", "mauritius", 0, 20);
        assertEquals(1, result.totalElements());
        assertEquals("Island Digital Studio", result.items().getFirst().dossier().getCompany().getName());
        assertEquals(SourceKind.FIXTURE, result.items().getFirst().dossier().getSource().getKind());
    }

    @Test
    void shouldTreatBlankFiltersAsAbsentAndUnknownFieldsAsNonMatches() {
        catalog.saveAll(List.of(item("a", "Island", null, null)));
        assertEquals(1, service.search(" ", " ", null, 0, 20).totalElements());
        assertEquals(0, service.search(null, "Software", null, 0, 20).totalElements());
        assertEquals(0, service.search(null, null, "Ebene", 0, 20).totalElements());
    }

    @Test
    void shouldPaginateInImportOrderWithoutOverflow() {
        catalog.saveAll(List.of(item("a", "First", null, null), item("b", "Second", null, null),
                item("c", "Third", null, null)));
        var result = service.search(null, null, null, 1, 2);
        assertEquals(3, result.totalElements());
        assertEquals(2, result.totalPages());
        assertEquals("Third", result.items().getFirst().dossier().getCompany().getName());
        assertTrue(service.search(null, null, null, Integer.MAX_VALUE, 100).items().isEmpty());
    }

    @Test
    void shouldValidatePagingAndFilterLength() {
        assertThrows(InvalidRequestException.class, () -> service.search(null, null, null, -1, 20));
        assertThrows(InvalidRequestException.class, () -> service.search(null, null, null, 0, 0));
        assertThrows(InvalidRequestException.class, () -> service.search(null, null, null, 0, 101));
        assertThrows(InvalidRequestException.class, () -> service.search("x".repeat(201), null, null, 0, 20));
    }

    @Test
    void shouldUpdateSameSourceAndKeepItsIdentifier() {
        catalog.saveAll(List.of(item("a", "Original", null, null)));
        UUID id = catalog.findAll().getFirst().id();
        catalog.saveAll(List.of(item("a", "Updated", null, null)));
        assertEquals(1, catalog.findAll().size());
        assertEquals(id, catalog.findAll().getFirst().id());
        assertEquals("Updated", service.find(id).dossier().getCompany().getName());
    }

    @Test
    void shouldKeepPossibleDuplicateAsSeparateDossierAndIgnoreFailedInput() {
        var original = item("a", "Island", null, null);
        var other = item("b", "Island", null, null);
        var duplicate = new SourceImportResult(other.request(), ImportStatus.POSSIBLE_DUPLICATE,
                other.dossier(), original.request().uri(), null);
        var failed = new SourceImportResult(new SourceImportRequest(URI.create("file:///missing.html"), SourceKind.FIXTURE),
                ImportStatus.READ_ERROR, null, null, "Missing file");
        catalog.saveAll(List.of(original, duplicate, failed));
        assertEquals(2, catalog.findAll().size());
        assertEquals(original.request().uri(), catalog.findAll().get(1).duplicateOf());
    }

    @Test
    void shouldReportUnknownIdentifier() {
        assertThrows(CompanyNotFoundException.class, () -> service.find(UUID.randomUUID()));
    }
}
