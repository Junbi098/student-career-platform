package com.studentcareer.source.reader;

import com.studentcareer.source.model.SourceDocument;
import com.studentcareer.source.model.SourceKind;
import java.io.IOException;
import java.net.URI;

public interface SourceReader {
    SourceDocument readSource(URI sourceUri, SourceKind kind) throws IOException;

    default String read(URI sourceUri) throws IOException {
        return readSource(sourceUri, SourceKind.LOCAL_DOCUMENT).html();
    }
}
