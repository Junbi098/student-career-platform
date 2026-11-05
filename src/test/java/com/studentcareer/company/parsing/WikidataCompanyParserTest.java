package com.studentcareer.company.parsing;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studentcareer.support.WikidataTestData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WikidataCompanyParserTest {
    private final WikidataCompanyParser parser = new WikidataCompanyParser();
    private final ObjectNode document = WikidataTestData.document("Q3301671");
    private final ObjectNode entity = (ObjectNode) document.path("entities").path("Q3301671");

    @Test
    void shouldExtractOnlyExplicitSupportedFields() {
        entity.withObject("/claims").putArray("P452").add(WikidataTestData.statement("Q418", true));
        var company = parser.parse(document.toString()).orElseThrow();
        assertEquals("Synthetic Island Network", company.getName());
        assertEquals("https://example.com", company.getWebsite());
        assertEquals("Mauritius", company.getLocation());
        assertNull(company.getIndustry());
    }

    @Test
    void shouldKeepUnknownWebsiteUnknown() {
        ((ObjectNode) entity.path("claims")).remove("P856");
        assertNull(parser.parse(document.toString()).orElseThrow().getWebsite());
    }

    @Test
    void shouldRejectUnsupportedOrUnsafeWebsite() {
        for (String value : new String[]{"javascript:alert(1)", "/relative", "https://user:password@example.com"}) {
            entity.withObject("/claims").putArray("P856").add(WikidataTestData.statement(value, false));
            assertNull(parser.parse(document.toString()).orElseThrow().getWebsite());
        }
    }

    @Test
    void shouldRequireCountryClaimInsteadOfInferringFromName() {
        entity.withObject("/claims").putArray("P17").add(WikidataTestData.statement("Q142", true));
        assertTrue(parser.parse(document.toString()).isEmpty());
    }

    @Test
    void shouldIgnoreDeprecatedAndEndedCountryClaims() {
        var country = (ObjectNode) entity.path("claims").path("P17").get(0);
        country.put("rank", "deprecated");
        assertTrue(parser.parse(document.toString()).isEmpty());
        country.put("rank", "normal");
        country.putObject("qualifiers").putArray("P582").addObject();
        assertTrue(parser.parse(document.toString()).isEmpty());
    }

    @Test
    void shouldPreferPreferredCountryOverNormalClaims() {
        var other = WikidataTestData.statement("Q142", true).put("rank", "preferred");
        ((com.fasterxml.jackson.databind.node.ArrayNode) entity.path("claims").path("P17")).add(other);
        assertTrue(parser.parse(document.toString()).isEmpty());
    }

    @Test
    void shouldSkipEntityWithDissolutionClaim() {
        entity.withObject("/claims").putArray("P576").add(WikidataTestData.statement("2020", false));
        assertTrue(parser.parse(document.toString()).isEmpty());
    }

    @Test
    void shouldUseFrenchLabelIfEnglishMissingAndNeverUseEntityIdAsName() {
        ((ObjectNode) entity.path("labels")).remove("en");
        entity.withObject("/labels").putObject("fr").put("value", "Entreprise fictive");
        assertEquals("Entreprise fictive", parser.parse(document.toString()).orElseThrow().getName());
        ((ObjectNode) entity.path("labels")).remove("fr");
        assertTrue(parser.parse(document.toString()).isEmpty());
    }

    @Test
    void shouldRejectMalformedOrMultipleEntities() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("not JSON"));
        document.withObject("/entities").putObject("Q2");
        assertThrows(IllegalArgumentException.class, () -> parser.parse(document.toString()));
    }
}
