package etl.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {
    private TimeUtil() {}

    private static final DateTimeFormatter STORE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static LocalDateTime toDateTime(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("timestamp value cannot be null");
        }
        if (value instanceof Number) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) value).longValue()), ZoneOffset.UTC);
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.isEmpty()) {
                throw new IllegalArgumentException("timestamp value cannot be blank");
            }
            return LocalDateTime.parse(text, STORE);
        }
        throw new IllegalArgumentException("unsupported timestamp type: " + value.getClass().getName());
    }

    public static String formatStore(Object value) {
        return toDateTime(value).format(STORE);
    }
}
