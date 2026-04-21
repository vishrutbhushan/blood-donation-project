package com.who.backend.repository;

import com.who.backend.dto.WhoEtlBankDTO;
import com.who.backend.dto.WhoEtlDonorDTO;
import com.who.backend.dto.WhoEtlInventoryTransactionDTO;
import java.util.List;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class WhoRepository {

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
        + "WHERE t.updated_at >= to_timestamp(? / 1000.0) "
        + "AND t.updated_at < to_timestamp(? / 1000.0) "
        + "ORDER BY t.updated_at, t.transaction_id";

    private static final String SQL_ETL_BANKS_DAY =
        "SELECT CAST(bb.bb_id AS text) AS bank_id, bb.name AS bank_name, bb.category, "
        + "bb.street AS address, bb.city, bb.state, bb.pincode, bb.phone, bb.email, "
        + "to_char(bb.created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
        + "to_char(bb.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM blood_bank bb "
        + "WHERE bb.updated_at::date = to_date(?, 'YYYY-MM-DD') "
        + "ORDER BY bb.bb_id";

    private static final String SQL_ETL_DONORS_DAY =
        "SELECT CAST(d.donor_id AS text) AS donor_id, d.name, d.blood_group, d.age, d.phone, "
        + "NULL::text AS email, NULL::text AS address_current, d.city AS city_current, d.state AS state_current, "
        + "d.pincode AS pincode_current, CAST(d.bb_id AS text) AS bank_id, "
        + "to_char(d.last_donated, 'YYYY-MM-DD') AS last_donated_on, "
        + "NULL::text AS last_donated_blood_bank, to_char(d.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM donor d "
        + "WHERE d.updated_at::date = to_date(?, 'YYYY-MM-DD') "
        + "ORDER BY d.donor_id";

    private static final String SQL_ETL_INVENTORY_TXN_DAY =
        "SELECT CAST(t.transaction_id AS text) AS transaction_id, t.source_event_id, CAST(t.bb_id AS text) AS bank_id, "
        + "CAST(t.donor_id AS text) AS donor_id, t.blood_group, t.component, t.transaction_type, "
        + "t.units_delta, t.running_balance_after, to_char(t.expiry_date, 'YYYY-MM-DD') AS expiry_date, "
        + "to_char(t.event_timestamp, 'YYYY-MM-DD HH24:MI:SS') AS event_timestamp, "
        + "to_char(t.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, false AS deleted "
        + "FROM inventory_transaction t "
        + "WHERE t.event_timestamp::date = to_date(?, 'YYYY-MM-DD') "
        + "ORDER BY t.event_timestamp, t.transaction_id";

    private static final String SQL_ETL_BANKS_MONTH =
        "SELECT CAST(bb.bb_id AS text) AS bank_id, bb.name AS bank_name, bb.category, "
        + "bb.street AS address, bb.city, bb.state, bb.pincode, bb.phone, bb.email, "
        + "to_char(bb.created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
        + "to_char(bb.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM blood_bank bb "
        + "WHERE to_char(bb.updated_at, 'YYYY-MM') = ? "
        + "ORDER BY bb.bb_id";

    private static final String SQL_ETL_DONORS_MONTH =
        "SELECT CAST(d.donor_id AS text) AS donor_id, d.name, d.blood_group, d.age, d.phone, "
        + "NULL::text AS email, NULL::text AS address_current, d.city AS city_current, d.state AS state_current, "
        + "d.pincode AS pincode_current, CAST(d.bb_id AS text) AS bank_id, "
        + "to_char(d.last_donated, 'YYYY-MM-DD') AS last_donated_on, "
        + "NULL::text AS last_donated_blood_bank, to_char(d.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM donor d "
        + "WHERE to_char(d.updated_at, 'YYYY-MM') = ? "
        + "ORDER BY d.donor_id";

    private static final String SQL_ETL_INVENTORY_TXN_MONTH =
        "SELECT CAST(t.transaction_id AS text) AS transaction_id, t.source_event_id, CAST(t.bb_id AS text) AS bank_id, "
        + "CAST(t.donor_id AS text) AS donor_id, t.blood_group, t.component, t.transaction_type, "
        + "t.units_delta, t.running_balance_after, to_char(t.expiry_date, 'YYYY-MM-DD') AS expiry_date, "
        + "to_char(t.event_timestamp, 'YYYY-MM-DD HH24:MI:SS') AS event_timestamp, "
        + "to_char(t.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, false AS deleted "
        + "FROM inventory_transaction t "
        + "WHERE to_char(t.event_timestamp, 'YYYY-MM') = ? "
        + "ORDER BY t.event_timestamp, t.transaction_id";

    private final JdbcTemplate jdbc;

    public WhoRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
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

    public List<WhoEtlBankDTO> fetchEtlBanksByDate(String date) {
        return jdbc.query(SQL_ETL_BANKS_DAY,
                ps -> ps.setString(1, date),
                BeanPropertyRowMapper.newInstance(WhoEtlBankDTO.class));
    }

    public List<WhoEtlDonorDTO> fetchEtlDonorsByDate(String date) {
        return jdbc.query(SQL_ETL_DONORS_DAY,
                ps -> ps.setString(1, date),
                BeanPropertyRowMapper.newInstance(WhoEtlDonorDTO.class));
    }

    public List<WhoEtlInventoryTransactionDTO> fetchEtlInventoryTransactionsByDate(String date) {
        return jdbc.query(SQL_ETL_INVENTORY_TXN_DAY,
                ps -> ps.setString(1, date),
                BeanPropertyRowMapper.newInstance(WhoEtlInventoryTransactionDTO.class));
    }

    public List<WhoEtlBankDTO> fetchEtlBanksByMonth(String month) {
        return jdbc.query(SQL_ETL_BANKS_MONTH,
                ps -> ps.setString(1, month),
                BeanPropertyRowMapper.newInstance(WhoEtlBankDTO.class));
    }

    public List<WhoEtlDonorDTO> fetchEtlDonorsByMonth(String month) {
        return jdbc.query(SQL_ETL_DONORS_MONTH,
                ps -> ps.setString(1, month),
                BeanPropertyRowMapper.newInstance(WhoEtlDonorDTO.class));
    }

    public List<WhoEtlInventoryTransactionDTO> fetchEtlInventoryTransactionsByMonth(String month) {
        return jdbc.query(SQL_ETL_INVENTORY_TXN_MONTH,
                ps -> ps.setString(1, month),
                BeanPropertyRowMapper.newInstance(WhoEtlInventoryTransactionDTO.class));
    }

}
