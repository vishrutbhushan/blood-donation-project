package etl.transform.redcross;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import etl.constants.Constants;
import etl.model.GeoPoint;
import etl.model.BloodBank;
import etl.model.Donor;
import etl.model.EtlBatch;
import etl.util.PincodeGeoMap;
import etl.util.TimeUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RedcrossTransformPipeline {
    private static final String BANK_RECORD_KEY = "centres";
    private static final String DONOR_RECORD_KEY = "people";
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    public EtlBatch run(Object payload, PincodeGeoMap geoMap) {
        EtlBatch out = new EtlBatch();
        JsonNode root = MAPPER.valueToTree(payload);
        for (JsonNode raw : pickRecords(root, BANK_RECORD_KEY)) {
            BloodBank bank = toBank(raw, geoMap);
            out.getBanks().add(bank);
        }
        for (JsonNode raw : pickRecords(root, DONOR_RECORD_KEY)) {
            Donor donor = toDonor(raw, geoMap);
            out.getDonors().add(donor);
        }
        return out;
    }

    private BloodBank toBank(JsonNode raw, PincodeGeoMap geoMap) {
        String bankId = required(raw, BANK_FIELDS.get("bank_id"));
        String bankName = required(raw, BANK_FIELDS.get("bank_name"));
        String pincode = required(raw, BANK_FIELDS.get("pincode"));
        GeoPoint geo = geo(pincode, geoMap);
        String updatedAt = TimeUtil.formatStore(required(raw, BANK_FIELDS.get("updated")));
        String createdAt = optional(raw, "created_at");
        String op = truthy(raw.get("deleted")) || truthy(raw.get("is_deleted")) ? Constants.OP_DELETE : Constants.OP_UPSERT;
        return BloodBank.builder()
            .source(Constants.SOURCE_REDCROSS)
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
                .lat(geo.getLat())
                .lon(geo.getLon())
            .updatedAt(updatedAt)
                .op(op)
                .build();
    }

    private Donor toDonor(JsonNode raw, PincodeGeoMap geoMap) {
        String donorId = required(raw, DONOR_FIELDS.get("donor_id"));
        String name = required(raw, DONOR_FIELDS.get("name"));
        String bloodGroup = required(raw, DONOR_FIELDS.get("blood_group"));
        String pincode = required(raw, DONOR_FIELDS.get("pincode_current"));
        GeoPoint geo = geo(pincode, geoMap);
        String updatedAt = TimeUtil.formatStore(required(raw, DONOR_FIELDS.get("updated")));
        String op = truthy(raw.get("deleted")) || truthy(raw.get("is_deleted")) ? Constants.OP_DELETE : Constants.OP_UPSERT;
        return Donor.builder()
            .source(Constants.SOURCE_REDCROSS)
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
                .lat(geo.getLat())
                .lon(geo.getLon())
                .bankId(optional(raw, DONOR_FIELDS.get("bank_id")))
                .lastDonatedOn(optional(raw, DONOR_FIELDS.get("last_donated_on")))
                .lastDonatedBloodBank(optional(raw, DONOR_FIELDS.get("last_donated_blood_bank")))
                .updatedAt(updatedAt)
                .op(op)
                .build();
    }

    private List<JsonNode> pickRecords(JsonNode payload, String key) {
        return list(payload.path(key));
    }

    private GeoPoint geo(String pin, PincodeGeoMap geoMap) {
        GeoPoint point = geoMap.get(pin);
        if (point == null) {
            return new GeoPoint(0.0, 0.0);
        }
        return point;
    }

    private String required(JsonNode raw, String key) {
        JsonNode v = raw.path(key);
        if (v.isMissingNode() || v.isNull() || v.asText().isBlank()) {
            throw new RuntimeException("missing required field: " + key);
        }
        return v.asText();
    }

    private String optional(JsonNode raw, String key) {
        JsonNode v = raw.path(key);
        if (v.isMissingNode() || v.isNull()) {
            return null;
        }
        return v.asText();
    }

    private boolean truthy(JsonNode v) {
        if (v == null || v.isMissingNode() || v.isNull()) {
            return false;
        }
        if (v.isBoolean()) {
            return v.asBoolean();
        }
        String s = v.asText().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "yes".equals(s);
    }

    private Integer toInteger(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.asInt();
        }
        String text = value.asText().trim();
        if (text.isEmpty()) {
            return null;
        }
        return Integer.parseInt(text);
    }

    private List<JsonNode> list(JsonNode value) {
        List<JsonNode> out = new ArrayList<>();
        if (value != null && value.isArray()) {
            for (JsonNode x : value) {
                if (x.isObject()) {
                    out.add(x);
                }
            }
        }
        return out;
    }

}
