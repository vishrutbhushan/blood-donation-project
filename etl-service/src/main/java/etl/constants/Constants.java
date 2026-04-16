package etl.constants;

public final class Constants {
    private Constants() {}

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static final String SOURCE_WHO = "who";
    public static final String SOURCE_REDCROSS = "redcross";

    public static final long INITIAL_PULL_START_TS = 1704067200000L;
    public static final String DATA_DIR = "/data";

    public static final String KEY_LAST_SYNC = "last_sync";

    public static final String OP_DELETE = "delete";
    public static final String OP_UPSERT = "upsert";

    public static final String WHO_BASE_URL = env("WHO_BASE_URL", "http://who-service:8082");
    public static final String WHO_INCREMENTAL_ENDPOINT = "/incremental";
    public static final String WHO_SINCE_PARAM = "since";
    public static final String WHO_UNTIL_PARAM = "until";

    public static final String REDCROSS_BASE_URL = env("REDCROSS_BASE_URL", "http://redcross-service:8081");
    public static final String REDCROSS_INCREMENTAL_ENDPOINT = "/incremental";
    public static final String REDCROSS_SINCE_PARAM = "since";
    public static final String REDCROSS_UNTIL_PARAM = "until";

    public static final String CLICKHOUSE_URL = env("CLICKHOUSE_URL", "http://clickhouse:8123");
    public static final String CLICKHOUSE_USER = env("CLICKHOUSE_USER", "default");
    public static final String CLICKHOUSE_PASSWORD = env("CLICKHOUSE_PASSWORD", "clickhouse");
    public static final String ELASTIC_URL = env("ELASTIC_URL", "http://elasticsearch:9200");
    public static final String ELASTIC_INDEX_BANKS = "bb_inventory_current";
    public static final String ELASTIC_INDEX_DONORS = "donor_availability_current";

}
