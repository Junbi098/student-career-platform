
package com.studentcareer.poc.demo;

import com.studentcareer.poc.CompanyHtmlParser;
import com.studentcareer.poc.domain.model.SourceReference;
import com.studentcareer.poc.fetch.LocalFileSourceFetcher;
import com.studentcareer.poc.output.CompanyDossierJsonWriter;
import com.studentcareer.poc.pipeline.CompanyDossierPipeline;
import com.studentcareer.poc.service.CompanyIntelligenceService;

import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public class CompanyDossierDemo {

    public static void main(String[] args) throws Exception {

        CompanyDossierPipeline pipeline =
                new CompanyDossierPipeline(
                        new CompanyIntelligenceService(
                                new LocalFileSourceFetcher(),
                                new CompanyHtmlParser()
                        ),
                        new CompanyDossierJsonWriter()
                );

        URL resource = CompanyDossierDemo.class.getResource(
                "/fixtures/company-complete.html"
        );

        if (resource == null) {
            throw new IllegalStateException(
                    "HTML fixture not found"
            );
        }

        // Reference fictive pour notre demonstration locale.
        // Aucune page web n'est consultee.
        SourceReference source = new SourceReference(
                URI.create("https://example.com/fictional-company"),
                "Fictional test source - not fetched",
                Instant.now()
        );

        String json = pipeline.execute(
                resource.toURI(),
                source
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
