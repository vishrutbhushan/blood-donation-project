package com.redcross.backend.repository;

import com.redcross.backend.dto.RedcrossCentreDTO;
import com.redcross.backend.dto.RedcrossDonorDTO;
import com.redcross.backend.dto.RedcrossEtlBankDTO;
import com.redcross.backend.dto.RedcrossEtlDonorDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedcrossRepository {

    private static final int MAX_LIMIT = 500;

    private static final String SQL_CENTRES_ALL =
        "SELECT bb.bb_id AS bb_id, bb.name, bb.category, bb.contact_number, bb.email, "
        + "bb.full_address, bb.postal_code, "
        + "(EXTRACT(EPOCH FROM bb.created_at) * 1000)::bigint AS created_at, "
        + "(EXTRACT(EPOCH FROM bb.updated_at) * 1000)::bigint AS updated_at, "
        + "false AS deleted "
        + "FROM blood_bank bb ORDER BY bb.bb_id";

    private static final String SQL_CENTRES_SINCE =
        "SELECT bb.bb_id AS bb_id, bb.name, bb.category, bb.contact_number, bb.email, "
        + "bb.full_address, bb.postal_code, "
        + "(EXTRACT(EPOCH FROM bb.created_at) * 1000)::bigint AS created_at, "
        + "(EXTRACT(EPOCH FROM bb.updated_at) * 1000)::bigint AS updated_at, "
        + "false AS deleted "
        + "FROM blood_bank bb "
        + "WHERE bb.updated_at >= to_timestamp(? / 1000.0) "
        + "ORDER BY bb.bb_id";

    private static final String SQL_INVENTORY_ALL =
        "SELECT bi.bb_id AS bb_id, bi.blood_group, bi.component, bi.quantity, "
        + "(EXTRACT(EPOCH FROM bi.updated_at) * 1000)::bigint AS updated_at "
        + "FROM blood_inventory bi ORDER BY bi.bb_id, bi.inventory_id";

    private static final String SQL_INVENTORY_SINCE =
        "SELECT bi.bb_id AS bb_id, bi.blood_group, bi.component, bi.quantity, "
        + "(EXTRACT(EPOCH FROM bi.updated_at) * 1000)::bigint AS updated_at "
        + "FROM blood_inventory bi "
        + "WHERE bi.bb_id IN (SELECT bb_id FROM blood_bank WHERE updated_at >= to_timestamp(? / 1000.0)) "
        + "ORDER BY bi.bb_id, bi.inventory_id";

    private static final String SQL_PEOPLE_ALL =
        "SELECT d.full_name, d.national_id, d.contact_number, d.address, d.blood_type, d.age, "
        + "to_char(d.last_donation_date, 'YYYY-MM-DD') AS last_donation_date, "
        + "(EXTRACT(EPOCH FROM d.created_at) * 1000)::bigint AS created_at, "
        + "(EXTRACT(EPOCH FROM d.updated_at) * 1000)::bigint AS updated_at, "
        + "false AS deleted "
        + "FROM blood_donor d ORDER BY d.donor_id";

    private static final String SQL_PEOPLE_SINCE =
        "SELECT d.full_name, d.national_id, d.contact_number, d.address, d.blood_type, d.age, "
        + "to_char(d.last_donation_date, 'YYYY-MM-DD') AS last_donation_date, "
        + "(EXTRACT(EPOCH FROM d.created_at) * 1000)::bigint AS created_at, "
        + "(EXTRACT(EPOCH FROM d.updated_at) * 1000)::bigint AS updated_at, "
        + "false AS deleted "
        + "FROM blood_donor d "
        + "WHERE d.updated_at >= to_timestamp(? / 1000.0) "
        + "ORDER BY d.donor_id";

    private static final String SQL_PEOPLE_FILTERED =
        "SELECT d.full_name, d.national_id, d.contact_number, d.address, d.blood_type, d.age, "
        + "to_char(d.last_donation_date, 'YYYY-MM-DD') AS last_donation_date, "
        + "(EXTRACT(EPOCH FROM d.created_at) * 1000)::bigint AS created_at, "
        + "(EXTRACT(EPOCH FROM d.updated_at) * 1000)::bigint AS updated_at, "
        + "b.postal_code AS pincode, false AS deleted "
        + "FROM blood_donor d "
        + "JOIN blood_bank b ON b.bb_id = d.bb_id "
        + "WHERE (? IS NULL OR d.blood_type = ?) "
        + "AND (? IS NULL OR b.postal_code = ?) "
        + "ORDER BY d.updated_at DESC LIMIT ?";

    private static final String SQL_ETL_BANKS_RANGE =
        "SELECT CAST(bb.bb_id AS text) AS bank_id, bb.name AS bank_name, bb.category, "
        + "bb.full_address AS address, NULL::text AS city, NULL::text AS state, bb.postal_code AS pincode, "
        + "bb.contact_number AS phone, bb.email, "
        + "to_char(bb.created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
        + "to_char(bb.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM blood_bank bb "
        + "WHERE bb.updated_at >= to_timestamp(? / 1000.0) "
        + "AND bb.updated_at < to_timestamp(? / 1000.0) "
        + "ORDER BY bb.bb_id";

    private static final String SQL_ETL_DONORS_RANGE =
        "SELECT CAST(d.donor_id AS text) AS donor_id, d.full_name AS name, d.blood_type AS blood_group, d.age, "
        + "d.contact_number AS phone, NULL::text AS email, d.address AS address_current, "
        + "NULL::text AS city_current, NULL::text AS state_current, b.postal_code AS pincode_current, "
        + "CAST(d.bb_id AS text) AS bank_id, to_char(d.last_donation_date, 'YYYY-MM-DD') AS last_donated_on, "
        + "NULL::text AS last_donated_blood_bank, to_char(d.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM blood_donor d "
        + "JOIN blood_bank b ON b.bb_id = d.bb_id "
        + "WHERE d.updated_at >= to_timestamp(? / 1000.0) "
        + "AND d.updated_at < to_timestamp(? / 1000.0) "
        + "ORDER BY d.donor_id";

    private final JdbcTemplate jdbc;

    public RedcrossRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ─── api_contracts: GET /api/redcross/centres (all + incremental) ─────────

    public List<RedcrossCentreDTO> fetchAllCentres() {
        return fetchCentresWithInventory(null);
    }

    public List<RedcrossCentreDTO> fetchCentresSince(long since) {
        return fetchCentresWithInventory(since);
    }

    private List<RedcrossCentreDTO> fetchCentresWithInventory(Long since) {
        List<RedcrossCentreRowDTO> banks = since == null
                ? jdbc.query(SQL_CENTRES_ALL, BeanPropertyRowMapper.newInstance(RedcrossCentreRowDTO.class))
                : jdbc.query(SQL_CENTRES_SINCE, ps -> ps.setLong(1, since),
                        BeanPropertyRowMapper.newInstance(RedcrossCentreRowDTO.class));

        if (banks.isEmpty()) {
            return new ArrayList<>();
        }

        List<RedcrossInventoryRowDTO> inventories = since == null
                ? jdbc.query(SQL_INVENTORY_ALL, BeanPropertyRowMapper.newInstance(RedcrossInventoryRowDTO.class))
                : jdbc.query(SQL_INVENTORY_SINCE, ps -> ps.setLong(1, since),
                        BeanPropertyRowMapper.newInstance(RedcrossInventoryRowDTO.class));

        Map<Long, RedcrossCentreDTO> bankMap = new LinkedHashMap<>();
        for (RedcrossCentreRowDTO bank : banks) {
            bankMap.put(bank.getBb_id(), bank);
        }

        for (RedcrossInventoryRowDTO inv : inventories) {
            RedcrossCentreDTO bank = bankMap.get(inv.getBb_id());
            if (bank != null) {
                bank.getBlood_inventory().add(inv);
            }
        }

        return new ArrayList<>(bankMap.values());
    }

    // ─── api_contracts: GET /api/redcross/people (all + incremental) ──────────

    public List<RedcrossDonorDTO> fetchAllPeople() {
        return fetchPeople(null);
    }

    public List<RedcrossDonorDTO> fetchPeopleFiltered(String bloodGroup, String pincode, int limit) {
        String bg = (bloodGroup == null || bloodGroup.isBlank()) ? null : bloodGroup;
        String pin = (pincode == null || pincode.isBlank()) ? null : pincode;
        int bounded = Math.max(1, Math.min(limit, MAX_LIMIT));

        return jdbc.query(SQL_PEOPLE_FILTERED,
                ps -> {
                    ps.setObject(1, bg);
                    ps.setObject(2, bg);
                    ps.setObject(3, pin);
                    ps.setObject(4, pin);
                    ps.setInt(5, bounded);
                },
                BeanPropertyRowMapper.newInstance(RedcrossDonorDTO.class));
    }

    public List<RedcrossDonorDTO> fetchPeopleSince(long since) {
        return fetchPeople(since);
    }

    private List<RedcrossDonorDTO> fetchPeople(Long since) {
        if (since == null) {
            return jdbc.query(SQL_PEOPLE_ALL, BeanPropertyRowMapper.newInstance(RedcrossDonorDTO.class));
        }
        return jdbc.query(SQL_PEOPLE_SINCE, ps -> ps.setLong(1, since),
                BeanPropertyRowMapper.newInstance(RedcrossDonorDTO.class));
    }

    // ─── ETL: GET /incremental?since=X&until=Y ────────────────────────────────
    // Returns {"centres": [...], "people": [...]} with ETL-compatible field names.
    // Field mapping:
    //   bank:  bank_id, bank_name, address, city(null), state(null), pincode, phone, update_time
    //   donor: donor_id, name, blood_group, phone, email(null), address_current,
    //          city_current(null), state_current(null), pincode_current (from bank), bank_id,
    //          last_donated_on, last_donated_blood_bank(null), update_time

    public List<RedcrossEtlBankDTO> fetchEtlBanks(long since, long until) {
        return jdbc.query(SQL_ETL_BANKS_RANGE,
                ps -> {
                    ps.setLong(1, since);
                    ps.setLong(2, until);
                },
                BeanPropertyRowMapper.newInstance(RedcrossEtlBankDTO.class));
    }

    public List<RedcrossEtlDonorDTO> fetchEtlDonors(long since, long until) {
        return jdbc.query(SQL_ETL_DONORS_RANGE,
                ps -> {
                    ps.setLong(1, since);
                    ps.setLong(2, until);
                },
                BeanPropertyRowMapper.newInstance(RedcrossEtlDonorDTO.class));
    }

    private static class RedcrossCentreRowDTO extends RedcrossCentreDTO {
        private Long bb_id;

        public Long getBb_id() {
            return bb_id;
        }

        public void setBb_id(Long bb_id) {
            this.bb_id = bb_id;
        }
    }

    private static class RedcrossInventoryRowDTO extends com.redcross.backend.dto.RedcrossInventoryDTO {
        private Long bb_id;

        public Long getBb_id() {
            return bb_id;
        }

        public void setBb_id(Long bb_id) {
            this.bb_id = bb_id;
        }
    }
}
