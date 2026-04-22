package etl.util;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

public final class JsonNodeUtil {
    private JsonNodeUtil() {}

    public static List<JsonNode> pickRecords(JsonNode payload, String key) {
        List<JsonNode> records = new ArrayList<>();
        JsonNode value = payload.path(key);
        if (!value.isArray()) {
            return records;
        }
        for (JsonNode node : value) {
            if (node.isObject()) {
                records.add(node);
            }
        }
        return records;
    }

    public static String required(JsonNode raw, String key) {
        JsonNode value = raw.path(key);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            throw new RuntimeException("missing required field: " + key);
        }
        return value.asText();
    }

    public static String optional(JsonNode raw, String key) {
        JsonNode value = raw.path(key);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    public static boolean truthy(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return false;
        }
        if (value.isBoolean()) {
            return value.asBoolean();
        }
        String text = value.asText().toLowerCase();
        return "true".equals(text) || "1".equals(text) || "yes".equals(text);
    }

    public static Integer toInteger(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asInt();
        }
        String text = value.asText().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }
}