package com.studentcareer.common.text;

import java.text.Normalizer;
import java.util.Locale;

public final class SearchText {
    private SearchText() { }
    public static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("(?U)\\s+", " ").strip().toLowerCase(Locale.ROOT);
    }
}
