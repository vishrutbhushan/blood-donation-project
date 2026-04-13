package com.redcross.backend.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

@Repository
public class RedcrossRepository {

    private static final DateTimeFormatter STORE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public RedcrossRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ─── api_contracts: GET /api/redcross/centres (all + incremental) ─────────

    public List<Map<String, Object>> fetchAllCentres() {
        return fetchCentresWithInventory(null);
    }

    public List<Map<String, Object>> fetchCentresSince(long since) {
        return fetchCentresWithInventory(since);
    }

    private List<Map<String, Object>> fetchCentresWithInventory(Long since) {
        String bankSql =
                "SELECT bb.bb_id, bb.name, bb.category, bb.contact_number, bb.email, "
                + "bb.full_address, bb.postal_code, bb.created_at, bb.updated_at "
                + "FROM blood_bank bb"
                + (since != null ? " WHERE bb.updated_at >= to_timestamp(? / 1000.0)" : "")
                + " ORDER BY bb.bb_id";

        String invSql =
                "SELECT bi.bb_id, bi.blood_group, bi.component, bi.quantity, bi.updated_at "
                + "FROM blood_inventory bi"
                + (since != null
                        ? " WHERE bi.bb_id IN (SELECT bb_id FROM blood_bank"
                          + " WHERE updated_at >= to_timestamp(? / 1000.0))"
                        : "")
                + " ORDER BY bi.bb_id, bi.inventory_id";

        Map<Long, Map<String, Object>> bankMap = new LinkedHashMap<>();

        RowCallbackHandler bankRch = rs -> {
            long bbId = rs.getLong("bb_id");
            Map<String, Object> bank = new LinkedHashMap<>();
            bank.put("name", rs.getString("name"));
            bank.put("category", rs.getString("category"));
            bank.put("contact_number", rs.getString("contact_number"));
            bank.put("email", rs.getString("email"));
            bank.put("full_address", rs.getString("full_address"));
            bank.put("postal_code", rs.getString("postal_code"));
            Timestamp c = rs.getTimestamp("created_at");
            bank.put("created_at", c != null ? c.toInstant().toEpochMilli() : null);
            Timestamp u = rs.getTimestamp("updated_at");
            bank.put("updated_at", u != null ? u.toInstant().toEpochMilli() : null);
            bank.put("blood_inventory", new ArrayList<>());
            bank.put("deleted", false);
            bankMap.put(bbId, bank);
        };

        if (since != null) {
            long s = since;
            jdbc.query(bankSql, ps -> ps.setLong(1, s), bankRch);
        } else {
            jdbc.query(bankSql, bankRch);
        }

        if (bankMap.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, List<Map<String, Object>>> invMap = new LinkedHashMap<>();

        RowCallbackHandler invRch = rs -> {
            long bbId = rs.getLong("bb_id");
            Map<String, Object> inv = new LinkedHashMap<>();
            inv.put("blood_group", rs.getString("blood_group"));
            inv.put("component", rs.getString("component"));
            inv.put("quantity", rs.getInt("quantity"));
            Timestamp t = rs.getTimestamp("updated_at");
            inv.put("updated_at", t != null ? t.toInstant().toEpochMilli() : null);
            invMap.computeIfAbsent(bbId, k -> new ArrayList<>()).add(inv);
        };

        if (since != null) {
            long s = since;
            jdbc.query(invSql, ps -> ps.setLong(1, s), invRch);
        } else {
            jdbc.query(invSql, invRch);
        }

        for (Map.Entry<Long, Map<String, Object>> e : bankMap.entrySet()) {
            e.getValue().put("blood_inventory",
                    invMap.getOrDefault(e.getKey(), new ArrayList<>()));
        }

        return new ArrayList<>(bankMap.values());
    }

    // ─── api_contracts: GET /api/redcross/people (all + incremental) ──────────

    public List<Map<String, Object>> fetchAllPeople() {
        return fetchPeople(null);
    }

    public List<Map<String, Object>> fetchPeopleSince(long since) {
        return fetchPeople(since);
    }

    private List<Map<String, Object>> fetchPeople(Long since) {
        String sql =
                "SELECT d.donor_id, d.full_name, d.national_id, d.contact_number, "
                + "d.address, d.blood_type, d.age, d.last_donation_date, d.created_at, d.updated_at "
                + "FROM blood_donor d"
                + (since != null ? " WHERE d.updated_at >= to_timestamp(? / 1000.0)" : "")
                + " ORDER BY d.donor_id";

        if (since != null) {
            long s = since;
            return jdbc.query(sql, ps -> ps.setLong(1, s), (rs, i) -> buildDonorRow(rs));
        } else {
            return jdbc.query(sql, (rs, i) -> buildDonorRow(rs));
        }
    }

    private Map<String, Object> buildDonorRow(ResultSet rs) throws SQLException {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("full_name", rs.getString("full_name"));
        m.put("national_id", rs.getString("national_id"));
        m.put("contact_number", rs.getString("contact_number"));
        m.put("address", rs.getString("address"));
        m.put("blood_type", rs.getString("blood_type"));
        m.put("age", rs.getInt("age"));
        java.sql.Date ldd = rs.getDate("last_donation_date");
        m.put("last_donation_date", ldd != null ? ldd.toString() : null);
        Timestamp c = rs.getTimestamp("created_at");
        m.put("created_at", c != null ? c.toInstant().toEpochMilli() : null);
        Timestamp u = rs.getTimestamp("updated_at");
        m.put("updated_at", u != null ? u.toInstant().toEpochMilli() : null);
        m.put("deleted", false);
        return m;
    }

    // ─── ETL: GET /incremental?since=X&until=Y ────────────────────────────────
    // Returns {"centres": [...], "people": [...]} with ETL-compatible field names.
    // Field mapping:
    //   bank:  bank_id, bank_name, address, city(null), state(null), pincode, phone, update_time
    //   donor: donor_id, name, blood_group, phone, email(null), address_current,
    //          city_current(null), state_current(null), pincode_current (from bank), bank_id,
    //          last_donated_on, last_donated_blood_bank(null), update_time

    public List<Map<String, Object>> fetchEtlBanks(long since, long until) {
        String sql =
                "SELECT bb.bb_id, bb.name, bb.category, bb.full_address, bb.postal_code, "
                + "bb.contact_number, bb.email, bb.created_at, bb.updated_at "
                + "FROM blood_bank bb "
                + "WHERE bb.updated_at >= to_timestamp(? / 1000.0) "
                + "  AND bb.updated_at < to_timestamp(? / 1000.0) "
                + "ORDER BY bb.bb_id";

        return jdbc.query(sql,
                ps -> { ps.setLong(1, since); ps.setLong(2, until); },
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("bank_id", String.valueOf(rs.getLong("bb_id")));
                    m.put("bank_name", rs.getString("name"));
                        m.put("category", rs.getString("category"));
                    m.put("address", rs.getString("full_address"));
                    m.put("city", null);
                    m.put("state", null);
                    m.put("pincode", rs.getString("postal_code"));
                    m.put("phone", rs.getString("contact_number"));
                        m.put("email", rs.getString("email"));
                        Timestamp createdTs = rs.getTimestamp("created_at");
                        m.put("created_at",
                            createdTs != null ? createdTs.toLocalDateTime().format(STORE_FMT) : null);
                    Timestamp ts = rs.getTimestamp("updated_at");
                    m.put("update_time",
                            ts != null ? ts.toLocalDateTime().format(STORE_FMT) : null);
                    m.put("deleted", false);
                    return m;
                });
    }

    public List<Map<String, Object>> fetchEtlDonors(long since, long until) {
        // Join to blood_bank to get postal_code as pincode_current (donors lack a separate pincode)
        String sql =
                "SELECT d.donor_id, d.full_name, d.blood_type, d.contact_number, d.age, "
                + "d.address, d.last_donation_date, d.updated_at, "
                + "CAST(d.bb_id AS TEXT) AS bb_id_str, b.postal_code "
                + "FROM blood_donor d "
                + "JOIN blood_bank b ON b.bb_id = d.bb_id "
                + "WHERE d.updated_at >= to_timestamp(? / 1000.0) "
                + "  AND d.updated_at < to_timestamp(? / 1000.0) "
                + "ORDER BY d.donor_id";

        return jdbc.query(sql,
                ps -> { ps.setLong(1, since); ps.setLong(2, until); },
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("donor_id", String.valueOf(rs.getLong("donor_id")));
                    m.put("name", rs.getString("full_name"));
                    m.put("blood_group", rs.getString("blood_type"));
                    m.put("age", rs.getInt("age"));
                    m.put("phone", rs.getString("contact_number"));
                    m.put("email", null);
                    m.put("address_current", rs.getString("address"));
                    m.put("city_current", null);
                    m.put("state_current", null);
                    m.put("pincode_current", rs.getString("postal_code"));
                    m.put("bank_id", rs.getString("bb_id_str"));
                    java.sql.Date ldd = rs.getDate("last_donation_date");
                    m.put("last_donated_on", ldd != null ? ldd.toString() : null);
                    m.put("last_donated_blood_bank", null);
                    Timestamp ts = rs.getTimestamp("updated_at");
                    m.put("update_time",
                            ts != null ? ts.toLocalDateTime().format(STORE_FMT) : null);
                    m.put("deleted", false);
                    return m;
                });
    }
}
