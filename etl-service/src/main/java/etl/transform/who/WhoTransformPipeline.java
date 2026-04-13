package etl.transform.who;

import etl.constants.Constants;
import etl.model.BloodBank;
import etl.model.Donor;
import etl.model.EtlBatch;
import etl.util.TimeUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("unchecked")
public class WhoTransformPipeline {
    private static final String BANK_RECORD_KEY = "blood_banks";
    private static final String DONOR_RECORD_KEY = "donors";

    private static final HashMap<String, String> BANK_FIELDS = new HashMap<>();
    private static final HashMap<String, String> DONOR_FIELDS = new HashMap<>();

    static {
        BANK_FIELDS.put("bank_id", "bank_id");
        BANK_FIELDS.put("bank_name", "bank_name");
        BANK_FIELDS.put("address", "address");
        BANK_FIELDS.put("city", "city");
        BANK_FIELDS.put("state", "state");
        BANK_FIELDS.put("pincode", "pincode");
        BANK_FIELDS.put("phone", "phone");
        BANK_FIELDS.put("updated", "update_time");

        DONOR_FIELDS.put("donor_id", "donor_id");
        DONOR_FIELDS.put("name", "name");
        DONOR_FIELDS.put("blood_group", "blood_group");
        DONOR_FIELDS.put("phone", "phone");
        DONOR_FIELDS.put("email", "email");
        DONOR_FIELDS.put("address_current", "address_current");
        DONOR_FIELDS.put("city_current", "city_current");
        DONOR_FIELDS.put("state_current", "state_current");
        DONOR_FIELDS.put("pincode_current", "pincode_current");
        DONOR_FIELDS.put("bank_id", "bank_id");
        DONOR_FIELDS.put("last_donated_on", "last_donated_on");
        DONOR_FIELDS.put("last_donated_blood_bank", "last_donated_blood_bank");
        DONOR_FIELDS.put("updated", "update_time");
    }

    public EtlBatch run(Object payload, Map<String, Object> geoMap) {
        EtlBatch out = new EtlBatch();
        for (Map<String, Object> raw : pickRecords(payload, BANK_RECORD_KEY)) {
            BloodBank bank = toBank(raw, geoMap);
            out.getBanks().add(bank);
        }
        for (Map<String, Object> raw : pickRecords(payload, DONOR_RECORD_KEY)) {
            Donor donor = toDonor(raw, geoMap);
            out.getDonors().add(donor);
        }
        return out;
    }

    private BloodBank toBank(Map<String, Object> raw, Map<String, Object> geoMap) {
        String bankId = required(raw, BANK_FIELDS.get("bank_id"));
        String bankName = required(raw, BANK_FIELDS.get("bank_name"));
        String pincode = required(raw, BANK_FIELDS.get("pincode"));
        Double[] geo = geo(pincode, geoMap);
        String updatedAt = TimeUtil.formatStore(required(raw, BANK_FIELDS.get("updated")));
        String createdAt = optional(raw, "created_at");
        String op = truthy(raw.get("deleted")) || truthy(raw.get("is_deleted")) ? Constants.OP_DELETE : Constants.OP_UPSERT;
        return BloodBank.builder()
            .source(Constants.SOURCE_WHO)
                .bankId(bankId)
                .bankName(bankName)
            .category(optional(raw, "category"))
                .address(optional(raw, BANK_FIELDS.get("address")))
                .city(optional(raw, BANK_FIELDS.get("city")))
                .state(optional(raw, BANK_FIELDS.get("state")))
                .pincode(pincode)
                .phone(optional(raw, BANK_FIELDS.get("phone")))
            .email(optional(raw, "email"))
            .createdAt(createdAt == null ? updatedAt : TimeUtil.formatStore(createdAt))
                .lat(geo[0])
                .lon(geo[1])
            .updatedAt(updatedAt)
                .op(op)
                .build();
    }

    private Donor toDonor(Map<String, Object> raw, Map<String, Object> geoMap) {
        String donorId = required(raw, DONOR_FIELDS.get("donor_id"));
        String name = required(raw, DONOR_FIELDS.get("name"));
        String bloodGroup = required(raw, DONOR_FIELDS.get("blood_group"));
        String pincode = required(raw, DONOR_FIELDS.get("pincode_current"));
        Double[] geo = geo(pincode, geoMap);
        String updatedAt = TimeUtil.formatStore(required(raw, DONOR_FIELDS.get("updated")));
        String op = truthy(raw.get("deleted")) || truthy(raw.get("is_deleted")) ? Constants.OP_DELETE : Constants.OP_UPSERT;
        return Donor.builder()
            .source(Constants.SOURCE_WHO)
                .donorId(donorId)
                .name(name)
                .bloodGroup(bloodGroup)
            .age(toInteger(raw.get("age")))
                .phone(optional(raw, DONOR_FIELDS.get("phone")))
                .email(optional(raw, DONOR_FIELDS.get("email")))
                .addressCurrent(optional(raw, DONOR_FIELDS.get("address_current")))
                .cityCurrent(optional(raw, DONOR_FIELDS.get("city_current")))
                .stateCurrent(optional(raw, DONOR_FIELDS.get("state_current")))
                .pincodeCurrent(pincode)
                .lat(geo[0])
                .lon(geo[1])
                .bankId(optional(raw, DONOR_FIELDS.get("bank_id")))
                .lastDonatedOn(optional(raw, DONOR_FIELDS.get("last_donated_on")))
                .lastDonatedBloodBank(optional(raw, DONOR_FIELDS.get("last_donated_blood_bank")))
                .updatedAt(updatedAt)
                .op(op)
                .build();
    }

    private List<Map<String, Object>> pickRecords(Object payload, String key) {
        if (payload instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) payload;
            return list(map.get(key));
        }
        return payload instanceof List ? list(payload) : new ArrayList<>();
    }

    private Double[] geo(String pin, Map<String, Object> geoMap) {
        if (!(geoMap.get(pin) instanceof Map)) {
            throw new RuntimeException("missing geo mapping for pincode: " + pin);
        }
        Map<String, Object> point = (Map<String, Object>) geoMap.get(pin);
        return new Double[] { toDouble(point.get("lat")), toDouble(point.get("lon")) };
    }

    private String required(Map<String, Object> raw, String key) {
        Object v = raw.get(key);
        if (v == null || String.valueOf(v).isBlank()) {
            throw new RuntimeException("missing required field: " + key);
        }
        return String.valueOf(v);
    }

    private String optional(Map<String, Object> raw, String key) {
        Object v = raw.get(key);
        return v == null ? null : String.valueOf(v);
    }

    private boolean truthy(Object v) {
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean) {
            return (Boolean) v;
        }
        String s = String.valueOf(v).toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }

    private Double toDouble(Object v) {
        if (!(v instanceof Number)) {
            throw new RuntimeException("geo value must be numeric");
        }
        return ((Number) v).doubleValue();
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private List<Map<String, Object>> list(Object value) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (value instanceof List) {
            for (Object x : (List<?>) value) {
                if (x instanceof Map) {
                    out.add((Map<String, Object>) x);
                }
            }
        }
        return out;
    }

}
