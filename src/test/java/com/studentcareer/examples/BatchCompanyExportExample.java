package com.studentcareer.examples;

import com.studentcareer.company.export.CompanyBatchJsonWriter;
import com.studentcareer.company.importing.CompanyBatchImportService;
import com.studentcareer.company.importing.SourceImportRequest;
import com.studentcareer.company.parsing.CompanyHtmlParser;
import com.studentcareer.company.service.CompanyAnalysisService;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.reader.LocalFileSourceReader;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class BatchCompanyExportExample {
    public static void main(String[] args) throws Exception {
        List<SourceImportRequest> requests;
        if (args.length == 0) {
            requests = List.of(
                    fixture("company-complete.html"),
                    fixture("company-duplicate.html"),
                    fixture("company-second.html"),
                    fixture("company-no-name.html"),
                    new SourceImportRequest(Path.of("target", "demo-input-missing.html").toUri(), SourceKind.FIXTURE),
                    new SourceImportRequest(URI.create("https://example.com/not-fetched"), SourceKind.FIXTURE));
        } else if (args.length == 2) {
            SourceKind kind = SourceKind.valueOf(args[1]);
            if (kind == SourceKind.UNCLASSIFIED) {
                throw new IllegalArgumentException("Choose FIXTURE or LOCAL_DOCUMENT");
            }
            try (var files = Files.list(Path.of(args[0]))) {
                requests = files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".html"))
                        .sorted().map(path -> new SourceImportRequest(path.toAbsolutePath().toUri(), kind)).toList();
            }
        } else {
            throw new IllegalArgumentException("Usage: no arguments, or <directory> <FIXTURE|LOCAL_DOCUMENT>");
        }
        var service = new CompanyAnalysisService(new LocalFileSourceReader(), new CompanyHtmlParser());
        var result = new CompanyBatchImportService(service).analyze(requests);
        String json = new CompanyBatchJsonWriter().write(result);
        Path output = Path.of("target", "demo-output", "company-batch.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, json, StandardCharsets.UTF_8);
        System.out.println(json);
        System.out.println("Batch JSON saved to: " + output.toAbsolutePath());
    }

    private static SourceImportRequest fixture(String name) throws Exception {
        URL resource = BatchCompanyExportExample.class.getResource("/demo/" + name);
        if (resource == null) {
            throw new IllegalStateException("Missing fixture: " + name);
        }
        return new SourceImportRequest(resource.toURI(), SourceKind.FIXTURE);
    }
}
