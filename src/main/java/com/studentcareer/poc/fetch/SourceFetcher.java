package com.studentcareer.poc.fetch;

import com.studentcareer.poc.domain.model.SourceKind;
import java.io.IOException;
import java.net.URI;

public interface SourceFetcher {
    FetchedSource fetchSource(URI sourceUri, SourceKind kind) throws IOException;

    default String fetch(URI sourceUri) throws IOException {
        return fetchSource(sourceUri, SourceKind.LOCAL_DOCUMENT).html();
    }
}
