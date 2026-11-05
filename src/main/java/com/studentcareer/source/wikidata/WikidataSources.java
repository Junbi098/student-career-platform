package com.studentcareer.source.wikidata;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Curated seed list, not an automated search of the entire Web. */
public final class WikidataSources {
    private WikidataSources() { }

    public static final String USER_AGENT =
            "StudentCareerPlatform/1.0 (https://github.com/Junbi098/student-career-platform)";
    public static final List<Source> SOURCES = List.of(
            source("Q3301671", "Mauritius Telecom"),
            source("Q3053417", "Emtel"),
            source("Q3301668", "Mauritius Commercial Bank"));
    public static final Set<URI> ALLOWED_URIS = SOURCES.stream().map(Source::url).collect(Collectors.toUnmodifiableSet());

    private static Source source(String id, String name) {
        return new Source(id, name, URI.create("https://www.wikidata.org/wiki/Special:EntityData/" + id + ".json"),
                "Wikidata", "CC0-1.0", URI.create("https://www.wikidata.org/wiki/Wikidata:Licensing"));
    }

    public record Source(String id, String name, URI url, String provider, String license, URI licenseUrl) { }
}
