package com.redcross.backend.repository;

import com.redcross.backend.dto.RedcrossEtlBankDTO;
import com.redcross.backend.dto.RedcrossEtlDonorDTO;
import com.redcross.backend.dto.RedcrossEtlInventoryTransactionDTO;
import java.util.List;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RedcrossRepository {

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
        + "FROM donor d "
        + "JOIN blood_bank b ON b.bb_id = d.bb_id "
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
        + "bb.full_address AS address, NULL::text AS city, NULL::text AS state, bb.postal_code AS pincode, "
        + "bb.contact_number AS phone, bb.email, "
        + "to_char(bb.created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
        + "to_char(bb.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM blood_bank bb "
        + "WHERE bb.updated_at::date = to_date(?, 'YYYY-MM-DD') "
        + "ORDER BY bb.bb_id";

    private static final String SQL_ETL_DONORS_DAY =
        "SELECT CAST(d.donor_id AS text) AS donor_id, d.full_name AS name, d.blood_type AS blood_group, d.age, "
        + "d.contact_number AS phone, NULL::text AS email, d.address AS address_current, "
        + "NULL::text AS city_current, NULL::text AS state_current, b.postal_code AS pincode_current, "
        + "CAST(d.bb_id AS text) AS bank_id, to_char(d.last_donation_date, 'YYYY-MM-DD') AS last_donated_on, "
        + "NULL::text AS last_donated_blood_bank, to_char(d.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM donor d "
        + "JOIN blood_bank b ON b.bb_id = d.bb_id "
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
        + "bb.full_address AS address, NULL::text AS city, NULL::text AS state, bb.postal_code AS pincode, "
        + "bb.contact_number AS phone, bb.email, "
        + "to_char(bb.created_at, 'YYYY-MM-DD HH24:MI:SS') AS created_at, "
        + "to_char(bb.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM blood_bank bb "
        + "WHERE to_char(bb.updated_at, 'YYYY-MM') = ? "
        + "ORDER BY bb.bb_id";

    private static final String SQL_ETL_DONORS_MONTH =
        "SELECT CAST(d.donor_id AS text) AS donor_id, d.full_name AS name, d.blood_type AS blood_group, d.age, "
        + "d.contact_number AS phone, NULL::text AS email, d.address AS address_current, "
        + "NULL::text AS city_current, NULL::text AS state_current, b.postal_code AS pincode_current, "
        + "CAST(d.bb_id AS text) AS bank_id, to_char(d.last_donation_date, 'YYYY-MM-DD') AS last_donated_on, "
        + "NULL::text AS last_donated_blood_bank, to_char(d.updated_at, 'YYYY-MM-DD HH24:MI:SS') AS update_time, "
        + "false AS deleted "
        + "FROM donor d "
        + "JOIN blood_bank b ON b.bb_id = d.bb_id "
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

    public RedcrossRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

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

    public List<RedcrossEtlInventoryTransactionDTO> fetchEtlInventoryTransactions(long since, long until) {
        return jdbc.query(SQL_ETL_INVENTORY_TXN_RANGE,
                ps -> {
                    ps.setLong(1, since);
                    ps.setLong(2, until);
                },
                BeanPropertyRowMapper.newInstance(RedcrossEtlInventoryTransactionDTO.class));
    }

    public List<RedcrossEtlBankDTO> fetchEtlBanksByDate(String date) {
        return jdbc.query(SQL_ETL_BANKS_DAY,
                ps -> ps.setString(1, date),
                BeanPropertyRowMapper.newInstance(RedcrossEtlBankDTO.class));
    }

    public List<RedcrossEtlDonorDTO> fetchEtlDonorsByDate(String date) {
        return jdbc.query(SQL_ETL_DONORS_DAY,
                ps -> ps.setString(1, date),
                BeanPropertyRowMapper.newInstance(RedcrossEtlDonorDTO.class));
    }

    public List<RedcrossEtlInventoryTransactionDTO> fetchEtlInventoryTransactionsByDate(String date) {
        return jdbc.query(SQL_ETL_INVENTORY_TXN_DAY,
                ps -> ps.setString(1, date),
                BeanPropertyRowMapper.newInstance(RedcrossEtlInventoryTransactionDTO.class));
    }

    public List<RedcrossEtlBankDTO> fetchEtlBanksByMonth(String month) {
        return jdbc.query(SQL_ETL_BANKS_MONTH,
                ps -> ps.setString(1, month),
                BeanPropertyRowMapper.newInstance(RedcrossEtlBankDTO.class));
    }

    public List<RedcrossEtlDonorDTO> fetchEtlDonorsByMonth(String month) {
        return jdbc.query(SQL_ETL_DONORS_MONTH,
                ps -> ps.setString(1, month),
                BeanPropertyRowMapper.newInstance(RedcrossEtlDonorDTO.class));
    }

    public List<RedcrossEtlInventoryTransactionDTO> fetchEtlInventoryTransactionsByMonth(String month) {
        return jdbc.query(SQL_ETL_INVENTORY_TXN_MONTH,
                ps -> ps.setString(1, month),
                BeanPropertyRowMapper.newInstance(RedcrossEtlInventoryTransactionDTO.class));
    }

}
