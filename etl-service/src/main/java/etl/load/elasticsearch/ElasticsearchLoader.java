package etl.load.elasticsearch;

import etl.constants.Constants;
import etl.model.BloodBank;
import etl.model.Donor;
import etl.util.JsonUtil;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class ElasticsearchLoader {
    private static final DateTimeFormatter STORE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JsonUtil jsonUtil;
    private final RestClient elastic;

    public ElasticsearchLoader(JsonUtil jsonUtil, RestClient.Builder builder) {
        this.jsonUtil = jsonUtil;
        this.elastic = builder.baseUrl(Constants.ELASTIC_URL).build();
    }

    public void loadBanks(List<BloodBank> banks) {
        if (banks == null || banks.isEmpty()) {
            return;
        }
        for (BloodBank b : banks) {
            String id = sourceAwareId(b.getSource(), b.getBankId());
            if (Constants.OP_DELETE.equalsIgnoreCase(str(b.getOp()))) {
                elastic.delete().uri("/{index}/_doc/{id}", Constants.ELASTIC_INDEX_BANKS, id).retrieve().toBodilessEntity();
            } else {
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("source", str(b.getSource()));
                doc.put("blood_bank_id", str(b.getBankId()));
                doc.put("contact_number", str(b.getPhone()));
                doc.put("blood_bank_name", str(b.getBankName()));
                doc.put("source_record_id", str(b.getBankId()));
                doc.put("event_time", isoDateTime(str(b.getUpdatedAt())));
                doc.put("city", str(b.getCity()));
                doc.put("state", str(b.getState()));
                doc.put("pincode", str(b.getPincode()));
                Map<String, Object> location = new LinkedHashMap<>();
                location.put("lat", b.getLat() == null ? 0.0 : b.getLat());
                location.put("lon", b.getLon() == null ? 0.0 : b.getLon());
                doc.put("location", location);
                doc.put("blood_group", "UNKNOWN");
                doc.put("component", "UNKNOWN");
                doc.put("units_available", 0);

                elastic.post()
                    .uri("/{index}/_doc/{id}", Constants.ELASTIC_INDEX_BANKS, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonUtil.toJson(doc))
                    .retrieve()
                    .toBodilessEntity();
            }
        }
    }

    public void loadDonors(List<Donor> donors) {
        if (donors == null || donors.isEmpty()) {
            return;
        }
        for (Donor d : donors) {
            String id = sourceAwareId(d.getSource(), d.getDonorId());
            if (Constants.OP_DELETE.equalsIgnoreCase(str(d.getOp()))) {
                elastic.delete().uri("/{index}/_doc/{id}", Constants.ELASTIC_INDEX_DONORS, id).retrieve().toBodilessEntity();
            } else {
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("source", str(d.getSource()));
                doc.put("source_record_id", str(d.getDonorId()));
                doc.put("contact_number", str(d.getPhone()));
                doc.put("event_time", isoDateTime(str(d.getUpdatedAt())));
                doc.put("blood_group", str(d.getBloodGroup()));
                Map<String, Object> location = new LinkedHashMap<>();
                location.put("lat", d.getLat() == null ? 0.0 : d.getLat());
                location.put("lon", d.getLon() == null ? 0.0 : d.getLon());
                doc.put("location", location);
                doc.put("last_donated_at", str(d.getLastDonatedOn()));
                doc.put("availability_status", true);

                elastic.post()
                    .uri("/{index}/_doc/{id}", Constants.ELASTIC_INDEX_DONORS, id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(jsonUtil.toJson(doc))
                    .retrieve()
                    .toBodilessEntity();
            }
        }
    }

    private String sourceAwareId(String source, String id) {
        return str(source) + ":" + str(id);
    }

    private String isoDateTime(String storeDateTime) {
        if (storeDateTime == null || storeDateTime.isBlank()) {
            return "1970-01-01T00:00:00Z";
        }
        LocalDateTime local = LocalDateTime.parse(storeDateTime, STORE);
        return local.atOffset(java.time.ZoneOffset.UTC).toInstant().toString();
    }

    private String str(Object v) {
        return v == null ? "" : String.valueOf(v);
    }
}
