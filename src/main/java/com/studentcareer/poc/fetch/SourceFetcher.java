
package com.studentcareer.poc.fetch;

import java.io.IOException;
import java.net.URI;

public interface SourceFetcher {

    String fetch(URI sourceUri) throws IOException;

}
