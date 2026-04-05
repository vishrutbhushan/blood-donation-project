package com.who.backend.repository;

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
public class WhoRepository {

    private static final DateTimeFormatter STORE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public WhoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ─── api_contracts: GET /api/who/blood-banks (all + incremental) ──────────

    public List<Map<String, Object>> fetchAllBloodBanks() {
        return fetchBloodBanksWithInventory(null);
    }

    public List<Map<String, Object>> fetchBloodBanksSince(long since) {
        return fetchBloodBanksWithInventory(since);
    }

    private List<Map<String, Object>> fetchBloodBanksWithInventory(Long since) {
        String bankSql =
                "SELECT bb.bb_id, bb.name, bb.category, bb.phone, bb.email, "
                + "bb.street, bb.city, bb.state, bb.pincode, bb.created_at, bb.updated_at "
                + "FROM blood_bank bb"
                + (since != null ? " WHERE bb.updated_at >= to_timestamp(? / 1000.0)" : "")
                + " ORDER BY bb.bb_id";

        String invSql =
                "SELECT bi.bb_id, bi.blood_group, bi.component_type, bi.units_available, "
                + "bi.last_updated "
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
            bank.put("phone", rs.getString("phone"));
            bank.put("email", rs.getString("email"));
            bank.put("street", rs.getString("street"));
            bank.put("city", rs.getString("city"));
            bank.put("state", rs.getString("state"));
            bank.put("pincode", rs.getString("pincode"));
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
            inv.put("component_type", rs.getString("component_type"));
            inv.put("units_available", rs.getInt("units_available"));
            Timestamp t = rs.getTimestamp("last_updated");
            inv.put("last_updated", t != null ? t.toInstant().toEpochMilli() : null);
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

    // ─── api_contracts: GET /api/who/donors (all + incremental) ──────────────

    public List<Map<String, Object>> fetchAllDonors() {
        return fetchDonors(null);
    }

    public List<Map<String, Object>> fetchDonorsSince(long since) {
        return fetchDonors(since);
    }

    private List<Map<String, Object>> fetchDonors(Long since) {
        String sql =
                "SELECT d.donor_id, d.name, d.aadhaar_hash, d.phone, d.city, d.state, "
                + "d.pincode, d.blood_group, d.age, d.last_donated, d.created_at, d.updated_at "
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
        m.put("name", rs.getString("name"));
        m.put("aadhaar_hash", rs.getString("aadhaar_hash"));
        m.put("phone", rs.getString("phone"));
        m.put("city", rs.getString("city"));
        m.put("state", rs.getString("state"));
        m.put("pincode", rs.getString("pincode"));
        m.put("blood_group", rs.getString("blood_group"));
        m.put("age", rs.getInt("age"));
        java.sql.Date ld = rs.getDate("last_donated");
        m.put("last_donated", ld != null ? ld.toString() : null);
        Timestamp c = rs.getTimestamp("created_at");
        m.put("created_at", c != null ? c.toInstant().toEpochMilli() : null);
        Timestamp u = rs.getTimestamp("updated_at");
        m.put("updated_at", u != null ? u.toInstant().toEpochMilli() : null);
        m.put("deleted", false);
        return m;
    }

    // ─── ETL: GET /incremental?since=X&until=Y ────────────────────────────────
    // Returns {"blood_banks": [...], "donors": [...]} with ETL-compatible field names.
    // Field mapping:
    //   bank:  bank_id, bank_name, address(street), city, state, pincode, phone, update_time
    //   donor: donor_id, name, blood_group, phone, email(null), address_current(null),
    //          city_current, state_current, pincode_current, bank_id,
    //          last_donated_on, last_donated_blood_bank(null), update_time

    public List<Map<String, Object>> fetchEtlBanks(long since, long until) {
        String sql =
                "SELECT bb.bb_id, bb.name, bb.street, bb.city, bb.state, bb.pincode, "
                + "bb.phone, bb.updated_at "
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
                    m.put("address", rs.getString("street"));
                    m.put("city", rs.getString("city"));
                    m.put("state", rs.getString("state"));
                    m.put("pincode", rs.getString("pincode"));
                    m.put("phone", rs.getString("phone"));
                    Timestamp ts = rs.getTimestamp("updated_at");
                    m.put("update_time",
                            ts != null ? ts.toLocalDateTime().format(STORE_FMT) : null);
                    m.put("deleted", false);
                    return m;
                });
    }

    public List<Map<String, Object>> fetchEtlDonors(long since, long until) {
        String sql =
                "SELECT d.donor_id, d.name, d.blood_group, d.phone, "
                + "d.city, d.state, d.pincode, d.last_donated, d.updated_at, "
                + "CAST(d.bb_id AS TEXT) AS bb_id_str "
                + "FROM blood_donor d "
                + "WHERE d.updated_at >= to_timestamp(? / 1000.0) "
                + "  AND d.updated_at < to_timestamp(? / 1000.0) "
                + "ORDER BY d.donor_id";

        return jdbc.query(sql,
                ps -> { ps.setLong(1, since); ps.setLong(2, until); },
                (rs, i) -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("donor_id", String.valueOf(rs.getLong("donor_id")));
                    m.put("name", rs.getString("name"));
                    m.put("blood_group", rs.getString("blood_group"));
                    m.put("phone", rs.getString("phone"));
                    m.put("email", null);
                    m.put("address_current", null);
                    m.put("city_current", rs.getString("city"));
                    m.put("state_current", rs.getString("state"));
                    m.put("pincode_current", rs.getString("pincode"));
                    m.put("bank_id", rs.getString("bb_id_str"));
                    java.sql.Date ld = rs.getDate("last_donated");
                    m.put("last_donated_on", ld != null ? ld.toString() : null);
                    m.put("last_donated_blood_bank", null);
                    Timestamp ts = rs.getTimestamp("updated_at");
                    m.put("update_time",
                            ts != null ? ts.toLocalDateTime().format(STORE_FMT) : null);
                    m.put("deleted", false);
                    return m;
                });
    }
}
