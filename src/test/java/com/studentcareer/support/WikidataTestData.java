package com.studentcareer.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Synthetic JSON, never a recorded live response. */
public final class WikidataTestData {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private WikidataTestData() { }

    public static ObjectNode document(String id) {
        ObjectNode root = MAPPER.createObjectNode();
        ObjectNode entity = root.putObject("entities").putObject(id);
        entity.put("id", id);
        entity.put("modified", "2020-01-02T03:04:05Z");
        entity.putObject("labels").putObject("en").put("value", "Synthetic Island Network");
        var claims = entity.putObject("claims");
        claims.putArray("P17").add(statement("Q1027", true));
        claims.putArray("P856").add(statement("https://example.com", false));
        return root;
    }

    public static ObjectNode statement(String value, boolean entity) {
        ObjectNode statement = MAPPER.createObjectNode();
        statement.put("rank", "normal");
        var data = statement.putObject("mainsnak").put("snaktype", "value").putObject("datavalue");
        if (entity) {
            data.putObject("value").put("id", value);
        } else {
            data.put("value", value);
        }
        return statement;
    }
}
