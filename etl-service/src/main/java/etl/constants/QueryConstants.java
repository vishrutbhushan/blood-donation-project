package etl.constants;

public final class QueryConstants {
    private QueryConstants() {}

    public static final String CLICKHOUSE_UPSERT_BANK = "INSERT INTO blood_bank_dim (bank_id, bank_name, address, city, state, pincode, phone, lat, lon, updated_at) VALUES ('%s','%s','%s','%s','%s','%s','%s',%s,%s,parseDateTimeBestEffort('%s'))";
    public static final String CLICKHOUSE_UPSERT_DONOR = "INSERT INTO donor_dim (donor_id, name, blood_group, phone, email, address_current, city_current, state_current, pincode_current, lat, lon, bank_id, last_donated_on, updated_at) VALUES ('%s','%s','%s','%s','%s','%s','%s','%s','%s',%s,%s,'%s','%s',parseDateTimeBestEffort('%s'))";

    public static final String CLICKHOUSE_DELETE_BANK = "ALTER TABLE blood_bank_dim DELETE WHERE bank_id = '%s'";
    public static final String CLICKHOUSE_DELETE_DONOR = "ALTER TABLE donor_dim DELETE WHERE donor_id = '%s'";
}
