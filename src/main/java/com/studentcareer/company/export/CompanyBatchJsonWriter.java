package com.studentcareer.company.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studentcareer.company.importing.BatchImportResult;
import com.studentcareer.company.importing.ImportStatus;
import com.studentcareer.company.importing.SourceImportResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CompanyBatchJsonWriter {
    private final ObjectMapper mapper = new ObjectMapper();
    private final CompanyDossierJsonWriter dossierWriter = new CompanyDossierJsonWriter();

    public String write(BatchImportResult batch) throws JsonProcessingException {
        Objects.requireNonNull(batch);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", batch.items().size());
        for (ImportStatus status : ImportStatus.values()) {
            summary.put(status.name(), batch.count(status));
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (SourceImportResult item : batch.items()) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("requestedUri", item.request().uri().toString());
            data.put("requestedKind", item.request().kind().name());
            data.put("status", item.status().name());
            data.put("duplicateOf", item.duplicateOf() == null ? null : item.duplicateOf().toString());
            data.put("message", item.message());
            data.put("dossier", item.dossier() == null ? null : dossierWriter.toData(item.dossier()));
            results.add(data);
        }
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("summary", summary);
        output.put("results", results);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output);
    }
}
