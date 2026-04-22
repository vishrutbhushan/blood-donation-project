package etl.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ElasticsearchUtil {
    private ElasticsearchUtil() {}

    private static final DateTimeFormatter STORE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static String sourceAwareId(String source, String id) {
        return str(source) + ":" + str(id);
    }

    public static String bankInventoryDocId(String source, String bankId, String bloodGroup, String component) {
        return sourceAwareId(source, bankId) + ":" + str(bloodGroup).replace(" ", "_") + ":" + str(component).replace(" ", "_");
    }

    public static String escJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String isoDateTime(String storeDateTime) {
        if (storeDateTime == null || storeDateTime.isBlank()) {
            return "1970-01-01T00:00:00Z";
        }
        LocalDateTime local = LocalDateTime.parse(storeDateTime, STORE);
        return local.atOffset(java.time.ZoneOffset.UTC).toInstant().toString();
    }

    public static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}