package etl.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class ClickhouseText {

    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");

    private ClickhouseText() {
    }

    public static String quote(String value) {
        return "'" + escape(value) + "'";
    }

    public static String dateTime(String value) {
        return "parseDateTimeBestEffort('" + escape(value) + "')";
    }

    public static String dateTimeFromMillis(long millis) {
        String value = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), KOLKATA)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        return dateTime(value);
    }

    public static String blank(String value) {
        return value == null ? "" : value;
    }

    public static String fallback(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private static String escape(String value) {
        return blank(value).replace("'", "''");
    }
}