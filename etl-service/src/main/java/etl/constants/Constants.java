package etl.constants;

public final class Constants {
    private Constants() {}

    public static final String SOURCE_WHO = "who";
    public static final String SOURCE_REDCROSS = "redcross";

    public static final long INITIAL_PULL_START_TS = 1704067200000L;
    public static final long INCREMENT_WINDOW_MS = 6L * 60L * 60L * 1000L;
    public static final long SCHEDULE_MS = 4L * 60L * 60L * 1000L;
    public static final String DATA_DIR = "/data";

    public static final String KEY_LAST_SYNC = "last_sync";

    public static final String OP_DELETE = "delete";
    public static final String OP_UPSERT = "upsert";

    public static final String WHO_BASE_URL = "http://host.docker.internal:8001";
    public static final String WHO_INCREMENTAL_ENDPOINT = "/incremental";
    public static final String WHO_SINCE_PARAM = "since";
    public static final String WHO_UNTIL_PARAM = "until";

    public static final String REDCROSS_BASE_URL = "http://host.docker.internal:8002";
    public static final String REDCROSS_INCREMENTAL_ENDPOINT = "/incremental";
    public static final String REDCROSS_SINCE_PARAM = "since";
    public static final String REDCROSS_UNTIL_PARAM = "until";

    public static final String CLICKHOUSE_URL = "http://host.docker.internal:8123";
    public static final String ELASTIC_URL = "http://host.docker.internal:9200";
    public static final String ELASTIC_INDEX_BANKS = "blood-banks";
    public static final String ELASTIC_INDEX_DONORS = "donors";

}
