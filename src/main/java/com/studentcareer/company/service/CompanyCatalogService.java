package com.studentcareer.company.service;

import com.studentcareer.common.exception.InvalidRequestException;
import com.studentcareer.company.domain.CompanyCatalogEntry;
import com.studentcareer.company.exception.CompanyNotFoundException;
import com.studentcareer.company.repository.CompanyRepository;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class CompanyCatalogService {
    private final CompanyRepository catalog;

    public CompanyCatalogService(CompanyRepository catalog) {
        this.catalog = catalog;
    }

    public CatalogPage search(String query, String industry, String location, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidRequestException("page must be >= 0 and size between 1 and 100");
        }
        String nameFilter = filter(query);
        String industryFilter = filter(industry);
        String locationFilter = filter(location);
        List<CompanyCatalogEntry> matching = catalog.findAll().stream()
                .filter(entry -> matches(entry.dossier().getCompany().getName(), nameFilter))
                .filter(entry -> matches(entry.dossier().getCompany().getIndustry(), industryFilter))
                .filter(entry -> matches(entry.dossier().getCompany().getLocation(), locationFilter))
                .toList();
        long offset = (long) page * size;
        int from = (int) Math.min(offset, matching.size());
        int to = Math.min(from + size, matching.size());
        return new CatalogPage(matching.subList(from, to), page, size, matching.size(),
                (int) ((matching.size() + (long) size - 1) / size));
    }

    public CompanyCatalogEntry find(UUID id) {
        return catalog.findById(id).orElseThrow(CompanyNotFoundException::new);
    }

    private String filter(String value) {
        if (value != null && value.length() > 200) {
            throw new InvalidRequestException("Search filters must contain at most 200 characters");
        }
        return normalize(value);
    }

    private boolean matches(String value, String filter) {
        return filter.isEmpty() || (value != null && normalize(value).contains(filter));
    }

    private String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFC)
                .replaceAll("(?U)\\s+", " ").strip().toLowerCase(Locale.ROOT);
    }

    public record CatalogPage(List<CompanyCatalogEntry> items, int page, int size,
                              long totalElements, int totalPages) {
        public CatalogPage {
            items = List.copyOf(items);
        }
    }
}
