package com.studentcareer.poc.demo;

import com.studentcareer.poc.CompanyHtmlParser;
import com.studentcareer.poc.batch.CompanyBatchService;
import com.studentcareer.poc.batch.SourceRequest;
import com.studentcareer.poc.domain.model.SourceKind;
import com.studentcareer.poc.fetch.LocalFileSourceFetcher;
import com.studentcareer.poc.output.CompanyBatchJsonWriter;
import com.studentcareer.poc.service.CompanyIntelligenceService;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class CompanyBatchDemo {
    public static void main(String[] args) throws Exception {
        List<SourceRequest> requests;
        if (args.length == 0) {
            requests = List.of(
                    fixture("company-complete.html"),
                    fixture("batch/company-duplicate.html"),
                    fixture("batch/company-second.html"),
                    fixture("batch/company-no-name.html"),
                    new SourceRequest(Path.of("target", "demo-input-missing.html").toUri(), SourceKind.FIXTURE),
                    new SourceRequest(URI.create("https://example.com/not-fetched"), SourceKind.FIXTURE));
        } else if (args.length == 2) {
            SourceKind kind = SourceKind.valueOf(args[1]);
            if (kind == SourceKind.UNCLASSIFIED) {
                throw new IllegalArgumentException("Choose FIXTURE or LOCAL_DOCUMENT");
            }
            try (var files = Files.list(Path.of(args[0]))) {
                requests = files.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".html"))
                        .sorted().map(path -> new SourceRequest(path.toAbsolutePath().toUri(), kind)).toList();
            }
        } else {
            throw new IllegalArgumentException("Usage: no arguments, or <directory> <FIXTURE|LOCAL_DOCUMENT>");
        }
        var service = new CompanyIntelligenceService(new LocalFileSourceFetcher(), new CompanyHtmlParser());
        var result = new CompanyBatchService(service).analyze(requests);
        String json = new CompanyBatchJsonWriter().write(result);
        Path output = Path.of("target", "demo-output", "company-batch.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, json, StandardCharsets.UTF_8);
        System.out.println(json);
        System.out.println("Batch JSON saved to: " + output.toAbsolutePath());
    }

    private static SourceRequest fixture(String name) throws Exception {
        URL resource = CompanyBatchDemo.class.getResource("/fixtures/" + name);
        if (resource == null) {
            throw new IllegalStateException("Missing fixture: " + name);
        }
        return new SourceRequest(resource.toURI(), SourceKind.FIXTURE);
    }
}
