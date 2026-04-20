package com.who.backend.repository;

import com.who.backend.dto.WhoBloodBankDTO;
import com.who.backend.dto.WhoDonorDTO;
import com.who.backend.dto.WhoEtlBankDTO;
import com.who.backend.dto.WhoEtlDonorDTO;
import com.who.backend.dto.WhoEtlInventoryTransactionDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WhoRepository {

    private static final int MAX_LIMIT = 500;

    private static final String SQL_BANKS_ALL =
        "SELECT bb.bb_id AS bb_id, bb.name, bb.category, bb.phone, bb.email, "
        + "bb.street, bb.city, bb.state, bb.pincode, "
        + "(EXTRACT(EPOCH FROM bb.created_at) * 1000)::bigint AS created_at, "
        + "(EXTRACT(EPOCH FROM bb.updated_at) * 1000)::bigint AS updated_at, "
        + "false AS deleted "
        + "FROM blood_bank bb ORDER BY bb.bb_id";

    private static final String SQL_BANKS_SINCE =
        "SELECT bb.bb_id AS bb_id, bb.name, bb.category, bb.phone, bb.email, "
        + "bb.street, bb.city, bb.state, bb.pincode, "
        + "(EXTRACT(EPOCH FROM bb.created_at) * 1000)::bigint AS created_at, "
        + "(EXTRACT(EPOCH FROM bb.updated_at) * 1000)::bigint AS updated_at, "
        + "false AS deleted "
        + "FROM blood_bank bb "
        + "WHERE bb.updated_at >= to_timestamp(? / 1000.0) "
        + "ORDER BY bb.bb_id";

    private static final String SQL_INVENTORY_ALL =
        "SELECT x.bb_id AS bb_id, x.blood_group, x.component AS component_type, x.running_balance_after AS units_available, "
        + "(EXTRACT(EPOCH FROM x.event_timestamp) * 1000)::bigint AS last_updated "
        + "FROM ("
        + "  SELECT DISTINCT ON (bi.bb_id, bi.blood_group, bi.component) "
        + "         bi.bb_id, bi.blood_group, bi.component, bi.running_balance_after, bi.event_timestamp "
        + "  FROM inventory_transaction bi "
        + "  ORDER BY bi.bb_id, bi.blood_group, bi.component, bi.event_timestamp DESC, bi.transaction_id DESC"
        + ") x "
        + "ORDER BY x.bb_id, x.blood_group, x.component";

    private static final String SQL_INVENTORY_SINCE =
        "SELECT x.bb_id AS bb_id, x.blood_group, x.component AS component_type, x.running_balance_after AS units_available, "
        + "(EXTRACT(EPOCH FROM x.event_timestamp) * 1000)::bigint AS last_updated "
        + "FROM ("
        + "  SELECT DISTINCT ON (bi.bb_id, bi.blood_group, bi.component) "
        + "         bi.bb_id, bi.blood_group, bi.component, bi.running_balance_after, bi.event_timestamp "
        + "  FROM inventory_transaction bi "
        + "  WHERE bi.event_timestamp >= to_timestamp(? / 1000.0) "
        + "  ORDER BY bi.bb_id, bi.blood_group, bi.component, bi.event_timestamp DESC, bi.transaction_id DESC"
        + ") x "
        + "ORDER BY x.bb_id, x.blood_group, x.component";

    private static final String SQL_DONORS_ALL =
        "SELECT d.name, d.abha_hash, d.phone, d.city, d.state, d.pincode, d.blood_group, d.age, "
        + "to_char(d.last_donated, 'YYYY-MM-DD') AS last_donated, "
        + "(EXTRACT(EPOCH FROM d.created_at) * 1000)::bigint AS created_at, "
        + "(EXTRACT(EPOCH FROM d.updated_at) * 1000)::bigint AS updated_at, "
        + "false AS deleted "
        + "FROM donor d ORDER BY d.donor_id";

    private static final String SQL_DONORS_SINCE =
        "SELECT d.name, d.abha_hash, d.phone, d.city, d.state, d.pincode, d.blood_group, d.age, "
        + "to_char(d.last_donated, 'YYYY-MM-DD') AS last_donated, "
        + "(EXTRACT(EPOCH FROM d.created_at) * 1000)::bigint AS created_at, "
        + "(EXTRACT(EPOCH FROM d.updated_at) * 1000)::bigint AS updated_at, "
        + "false AS deleted "
        + "FROM donor d "
        + "WHERE d.updated_at >= to_timestamp(? / 1000.0) "
        + "ORDER BY d.donor_id";

    private static final String SQL_DONORS_FILTERED =
        "SELECT d.name, d.abha_hash, d.phone, d.city, d.state, d.pincode, d.blood_group, d.age, "
        + "to_char(d.last_donated, 'YYYY-MM-DD') AS last_donated, "
        + "(EXTRACT(EPOCH FROM d.created_at) * 1000)::bigint AS created_at, "
        + "(EXTRACT(EPOCH FROM d.updated_at) * 1000)::bigint AS updated_at, "
        + "false AS deleted "
        + "FROM donor d "
        + "WHERE (? IS NULL OR d.blood_group = ?) "
        + "AND (? IS NULL OR d.pincode = ?) "
        + "ORDER BY d.updated_at DESC LIMIT ?";

    private static final String SQL_ETL_BANKS_RANGE =
        "SELECT CAST(bb.bb_id AS text) AS bank_id, bb.name AS bank_name, bb.category, "
        + "bb.street AS address, bb.city, bb.state, bb.pincode, bb.phone, bb.email, "
        + "to_char(bb.created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
        + "to_char(bb.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM blood_bank bb "
        + "WHERE bb.updated_at >= to_timestamp(? / 1000.0) "
        + "AND bb.updated_at < to_timestamp(? / 1000.0) "
        + "ORDER BY bb.bb_id";

    private static final String SQL_ETL_DONORS_RANGE =
        "SELECT CAST(d.donor_id AS text) AS donor_id, d.name, d.blood_group, d.age, d.phone, "
        + "NULL::text AS email, NULL::text AS address_current, d.city AS city_current, d.state AS state_current, "
        + "d.pincode AS pincode_current, CAST(d.bb_id AS text) AS bank_id, "
        + "to_char(d.last_donated, 'YYYY-MM-DD') AS last_donated_on, "
        + "NULL::text AS last_donated_blood_bank, to_char(d.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM donor d "
        + "WHERE d.updated_at >= to_timestamp(? / 1000.0) "
        + "AND d.updated_at < to_timestamp(? / 1000.0) "
        + "ORDER BY d.donor_id";

    private static final String SQL_ETL_INVENTORY_TXN_RANGE =
        "SELECT CAST(t.transaction_id AS text) AS transaction_id, t.source_event_id, CAST(t.bb_id AS text) AS bank_id, "
        + "CAST(t.donor_id AS text) AS donor_id, t.blood_group, t.component, t.transaction_type, "
        + "t.units_delta, t.running_balance_after, to_char(t.expiry_date, 'YYYY-MM-DD') AS expiry_date, "
        + "to_char(t.event_timestamp, 'YYYY-MM-DD HH24:MI:SS') AS event_timestamp, "
        + "to_char(t.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, false AS deleted "
        + "FROM inventory_transaction t "
        + "WHERE t.event_timestamp >= to_timestamp(? / 1000.0) "
        + "AND t.event_timestamp < to_timestamp(? / 1000.0) "
        + "ORDER BY t.transaction_id";

    private final JdbcTemplate jdbc;

    public WhoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<WhoBloodBankDTO> fetchAllBloodBanks() {
        return fetchBloodBanksWithInventory(null);
    }

    public List<WhoBloodBankDTO> fetchBloodBanksSince(long since) {
        return fetchBloodBanksWithInventory(since);
    }

    private List<WhoBloodBankDTO> fetchBloodBanksWithInventory(Long since) {
        List<WhoBloodBankRowDTO> banks = since == null
                ? jdbc.query(SQL_BANKS_ALL, BeanPropertyRowMapper.newInstance(WhoBloodBankRowDTO.class))
                : jdbc.query(SQL_BANKS_SINCE, ps -> ps.setLong(1, since),
                        BeanPropertyRowMapper.newInstance(WhoBloodBankRowDTO.class));

        if (banks.isEmpty()) {
            return new ArrayList<>();
        }

        List<WhoInventoryRowDTO> inventories = since == null
                ? jdbc.query(SQL_INVENTORY_ALL, BeanPropertyRowMapper.newInstance(WhoInventoryRowDTO.class))
                : jdbc.query(SQL_INVENTORY_SINCE, ps -> ps.setLong(1, since),
                        BeanPropertyRowMapper.newInstance(WhoInventoryRowDTO.class));

        Map<Long, WhoBloodBankDTO> bankMap = new LinkedHashMap<>();
        for (WhoBloodBankRowDTO bank : banks) {
            bankMap.put(bank.getBb_id(), bank);
        }

        for (WhoInventoryRowDTO inv : inventories) {
            WhoBloodBankDTO bank = bankMap.get(inv.getBb_id());
            if (bank != null) {
                bank.getBlood_inventory().add(inv);
            }
        }

        return new ArrayList<>(bankMap.values());
    }

    public List<WhoDonorDTO> fetchAllDonors() {
        return fetchDonors(null);
    }

    public List<WhoDonorDTO> fetchDonorsFiltered(String bloodGroup, String pincode, int limit) {
        String bg = (bloodGroup == null || bloodGroup.isBlank()) ? null : bloodGroup;
        String pin = (pincode == null || pincode.isBlank()) ? null : pincode;
        int bounded = Math.max(1, Math.min(limit, MAX_LIMIT));

        return jdbc.query(SQL_DONORS_FILTERED,
                ps -> {
                    ps.setObject(1, bg);
                    ps.setObject(2, bg);
                    ps.setObject(3, pin);
                    ps.setObject(4, pin);
                    ps.setInt(5, bounded);
                },
                BeanPropertyRowMapper.newInstance(WhoDonorDTO.class));
    }

    public List<WhoDonorDTO> fetchDonorsSince(long since) {
        return fetchDonors(since);
    }

    private List<WhoDonorDTO> fetchDonors(Long since) {
        if (since == null) {
            return jdbc.query(SQL_DONORS_ALL, BeanPropertyRowMapper.newInstance(WhoDonorDTO.class));
        }
        return jdbc.query(SQL_DONORS_SINCE, ps -> ps.setLong(1, since),
                BeanPropertyRowMapper.newInstance(WhoDonorDTO.class));
    }

    public List<WhoEtlBankDTO> fetchEtlBanks(long since, long until) {
        return jdbc.query(SQL_ETL_BANKS_RANGE,
                ps -> {
                    ps.setLong(1, since);
                    ps.setLong(2, until);
                },
                BeanPropertyRowMapper.newInstance(WhoEtlBankDTO.class));
    }

    public List<WhoEtlDonorDTO> fetchEtlDonors(long since, long until) {
        return jdbc.query(SQL_ETL_DONORS_RANGE,
                ps -> {
                    ps.setLong(1, since);
                    ps.setLong(2, until);
                },
                BeanPropertyRowMapper.newInstance(WhoEtlDonorDTO.class));
    }

    public List<WhoEtlInventoryTransactionDTO> fetchEtlInventoryTransactions(long since, long until) {
        return jdbc.query(SQL_ETL_INVENTORY_TXN_RANGE,
                ps -> {
                    ps.setLong(1, since);
                    ps.setLong(2, until);
                },
                BeanPropertyRowMapper.newInstance(WhoEtlInventoryTransactionDTO.class));
    }

    private static class WhoBloodBankRowDTO extends WhoBloodBankDTO {
        private Long bb_id;

        public Long getBb_id() {
            return bb_id;
        }

        @SuppressWarnings("unused")
        public void setBb_id(Long bb_id) {
            this.bb_id = bb_id;
        }
    }

    private static class WhoInventoryRowDTO extends com.who.backend.dto.WhoInventoryDTO {
        private Long bb_id;

        public Long getBb_id() {
            return bb_id;
        }

        @SuppressWarnings("unused")
        public void setBb_id(Long bb_id) {
            this.bb_id = bb_id;
        }
    }
}
