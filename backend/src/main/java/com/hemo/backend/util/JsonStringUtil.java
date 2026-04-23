package com.hemo.backend.util;

import java.util.ArrayList;
import java.util.List;

public final class JsonStringUtil {
    private JsonStringUtil() {}

    public static String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String joinQuotedJsonValues(List<String> values) {
        List<String> quoted = new ArrayList<>();
        for (String value : values) {
            quoted.add("\"" + escapeJson(value) + "\"");
        }
        return String.join(", ", quoted);
    }

    public static String emptyToBlank(String value) {
        return value == null ? "" : value;
    }

    public static String emptyToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}