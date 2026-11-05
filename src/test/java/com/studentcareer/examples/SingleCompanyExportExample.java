package com.studentcareer.examples;

import com.studentcareer.company.export.CompanyDossierExportService;
import com.studentcareer.company.export.CompanyDossierJsonWriter;
import com.studentcareer.company.parsing.CompanyHtmlParser;
import com.studentcareer.company.service.CompanyAnalysisService;
import com.studentcareer.source.model.SourceKind;
import com.studentcareer.source.reader.LocalFileSourceReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SingleCompanyExportExample {

    public static void main(String[] args) throws Exception {

        CompanyDossierExportService pipeline =
                new CompanyDossierExportService(
                        new CompanyAnalysisService(
                                new LocalFileSourceReader(),
                                new CompanyHtmlParser()
                        ),
                        new CompanyDossierJsonWriter()
                );

        URL resource = SingleCompanyExportExample.class.getResource(
                "/demo/company-complete.html"
        );

        if (resource == null) {
            throw new IllegalStateException(
                    "HTML fixture not found"
            );
        }

        String json = pipeline.execute(
                resource.toURI(),
                SourceKind.FIXTURE
        ).orElseThrow(() -> new IllegalStateException(
                "No company found in HTML"
        ));

        System.out.println("=== COMPANY DOSSIER JSON ===");
        System.out.println(json);

        Path output = Path.of(
                "target", "demo-output", "company-dossier.json"
        );

        Files.createDirectories(output.getParent());

        Files.writeString(
                output,
                json,
                StandardCharsets.UTF_8
        );

        System.out.println();
        System.out.println(
                "JSON saved to: " + output.toAbsolutePath()
        );
    }
}
