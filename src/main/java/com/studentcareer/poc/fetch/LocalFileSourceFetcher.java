
package com.studentcareer.poc.fetch;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalFileSourceFetcher implements SourceFetcher {

    @Override
    public String fetch(URI sourceUri) throws IOException {

        if (sourceUri == null) {
            throw new IllegalArgumentException(
                    "Source URI cannot be null"
            );
        }

        if (!"file".equalsIgnoreCase(sourceUri.getScheme())) {
            throw new IllegalArgumentException(
                    "Only local file URIs are supported"
            );
        }

        Path path = Path.of(sourceUri);

        return Files.readString(
                path,
                StandardCharsets.UTF_8
        );
    }
}
