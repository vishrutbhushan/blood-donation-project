package etl.util;

import etl.constants.Constants;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

public final class ClickhouseUtil {
    private ClickhouseUtil() {}

    public static int sourceId(String source) {
        if (Constants.SOURCE_REDCROSS.equalsIgnoreCase(source)) {
            return 1;
        }
        if (Constants.SOURCE_WHO.equalsIgnoreCase(source)) {
            return 2;
        }
        throw new IllegalArgumentException("Unknown source: " + source);
    }

    public static int bloodGroupId(String bloodGroup) {
        String normalized = normalizeBloodGroup(bloodGroup);
        return switch (normalized) {
            case "A+" -> 1;
            case "A-" -> 2;
            case "B+" -> 3;
            case "B-" -> 4;
            case "AB+" -> 5;
            case "AB-" -> 6;
            case "O+" -> 7;
            case "O-" -> 8;
            case "BOMBAY", "HH", "OH" -> 9;
            case "RH NULL", "RHNULL", "RH-NULL" -> 10;
            default -> throw new IllegalArgumentException("Unknown blood group: " + bloodGroup);
        };
    }

    public static String normalizeBloodGroup(String bloodGroup) {
        if (bloodGroup == null || bloodGroup.isBlank()) {
            return "UNKNOWN";
        }
        return bloodGroup.trim().toUpperCase(Locale.ROOT);
    }

    public static String normalizeComponent(String component) {
        if (component == null || component.isBlank()) {
            return "";
        }
        return component.trim().toUpperCase(Locale.ROOT);
    }

    public static boolean isDelete(String op) {
        return Constants.OP_DELETE.equalsIgnoreCase(op);
    }

    public static long bankId(String source, String sourceBankId) {
        return stableUInt64("bank:" + source + ":" + sourceBankId);
    }

    public static long donorSk(String source, String donorId) {
        return stableUInt64("donor:" + source + ":" + donorId);
    }

    public static long locationId(String source, String pincode, String address) {
        return stableUInt64("loc:" + source + ":" + ClickhouseText.blank(pincode) + ":" + ClickhouseText.blank(address));
    }

    public static long stableUInt64(String key) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
            return ByteBuffer.wrap(bytes).getLong() & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static long versionOf(String updatedAt) {
        return TimeUtil.toDateTime(updatedAt).toInstant(ZoneOffset.UTC).toEpochMilli();
    }

    public static int toEventTimeId(LocalDateTime dateTime) {
        return (int) dateTime.toEpochSecond(ZoneOffset.UTC);
    }

    public static int toEventDateId(LocalDateTime dateTime) {
        return Integer.parseInt(dateTime.toLocalDate().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));
    }

    public static boolean eligibleDonor(String lastDonatedOn, LocalDateTime updatedAt) {
        if (lastDonatedOn == null || lastDonatedOn.isBlank()) {
            return true;
        }
        LocalDate last = LocalDate.parse(lastDonatedOn);
        return last.plusDays(90).isBefore(updatedAt.toLocalDate()) || last.plusDays(90).isEqual(updatedAt.toLocalDate());
    }

    public static int age(Integer age) {
        if (age == null || age < 0) {
            return 0;
        }
        return Math.min(age, 120);
    }

    public static String dateOrDefault(String dateText) {
        if (dateText == null || dateText.isBlank()) {
            return "toDate('1970-01-01')";
        }
        return "toDate('" + dateText + "')";
    }

    public static String identityHash(String source, String donorId, String phone) {
        return source + ":" + ClickhouseText.blank(donorId) + ":" + ClickhouseText.blank(phone);
    }

    public static String counterTuple(String updatedAt, String source, String apiName, String recordType, int count) {
        LocalDateTime at = TimeUtil.toDateTime(updatedAt).truncatedTo(ChronoUnit.HOURS);
        String eventHour = ClickhouseText.dateTime(at.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return "(" + eventHour + "," + ClickhouseText.quote(source) + "," + ClickhouseText.quote(apiName) + ","
            + ClickhouseText.quote(recordType) + "," + count + ")";
    }
}