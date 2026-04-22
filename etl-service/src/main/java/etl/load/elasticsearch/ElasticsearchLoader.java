package etl.load.elasticsearch;

import etl.constants.Constants;
import etl.model.BloodBank;
import etl.model.Donor;
import etl.model.InventoryTransaction;
import etl.util.JsonUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientResponseException;

import static etl.util.ElasticsearchUtil.bankInventoryDocId;
import static etl.util.ElasticsearchUtil.escJson;
import static etl.util.ElasticsearchUtil.isoDateTime;
import static etl.util.ElasticsearchUtil.sourceAwareId;
import static etl.util.ElasticsearchUtil.str;

@Component
public class ElasticsearchLoader {
    private static final Logger log = LoggerFactory.getLogger(ElasticsearchLoader.class);
    private static final int BULK_BATCH_SIZE = 500;
    private static final MediaType NDJSON = MediaType.valueOf("application/x-ndjson");
        private static final String BANK_TEMPLATE = """
                {
                    "index_patterns": ["bb_inventory_current*"],
                    "template": {
                        "settings": {
                            "number_of_shards": 3,
                            "number_of_replicas": 1,
                            "refresh_interval": "5s"
                        },
                        "mappings": {
                            "dynamic": "strict",
                            "properties": {
                                "source": { "type": "keyword" },
                                "blood_bank_id": { "type": "keyword" },
                                "contact_number": { "type": "keyword" },
                                "blood_bank_name": { "type": "keyword" },
                                "source_record_id": { "type": "keyword" },
                                "event_time": { "type": "date" },
                                "address": { "type": "keyword" },
                                "city": { "type": "keyword" },
                                "state": { "type": "keyword" },
                                "pincode": { "type": "keyword" },
                                "location": { "type": "geo_point" },
                                "category": { "type": "keyword" },
                                "email": { "type": "keyword" },
                                "blood_group": { "type": "keyword" },
                                "component": { "type": "keyword" },
                                "units_available": { "type": "integer" }
                            }
                        }
                    }
                }
                """;
        private static final String DONOR_TEMPLATE = """
                {
                    "index_patterns": ["donor_availability_current*"],
                    "template": {
                        "settings": {
                            "number_of_shards": 3,
                            "number_of_replicas": 1,
                            "refresh_interval": "5s"
                        },
                        "mappings": {
                            "dynamic": "strict",
                            "properties": {
                                "source": { "type": "keyword" },
                                "source_record_id": { "type": "keyword" },
                                "name": { "type": "keyword" },
                                "contact_number": { "type": "keyword" },
                                "event_time": { "type": "date" },
                                "blood_group": { "type": "keyword" },
                                "pincode": { "type": "keyword" },
                                "city": { "type": "keyword" },
                                "state": { "type": "keyword" },
                                "location": { "type": "geo_point" },
                                "last_donated_at": { "type": "date" },
                                "availability_status": { "type": "boolean" }
                            }
                        }
                    }
                }
                """;

    private final JsonUtil jsonUtil;
    private final RestClient elastic;

    public ElasticsearchLoader(JsonUtil jsonUtil, RestClient.Builder builder) {
        this.jsonUtil = jsonUtil;
        this.elastic = builder.baseUrl(Objects.requireNonNull(Constants.ELASTIC_URL, "ELASTIC_URL")).build();
    }

    public void loadBanks(List<BloodBank> banks, List<InventoryTransaction> currentInventoryState) {
        log.info("api.enter ElasticsearchLoader.loadBanks");
        if (banks == null || banks.isEmpty()) {
            log.info("api.exit ElasticsearchLoader.loadBanks");
            return;
        }

        Map<String, List<InventoryTransaction>> inventoryByBank = new HashMap<>();
        if (currentInventoryState != null) {
            for (InventoryTransaction txn : currentInventoryState) {
                String source = str(txn.getSource());
                String bankId = str(txn.getBankId());
                if (source.isBlank() || bankId.isBlank()) {
                    continue;
                }
                String key = sourceAwareId(source, bankId);
                inventoryByBank.computeIfAbsent(key, k -> new ArrayList<>()).add(txn);
            }
        }

        List<String> bulkLines = new ArrayList<>();

        for (BloodBank b : banks) {
            String bankKey = sourceAwareId(b.getSource(), b.getBankId());
            if (Constants.OP_DELETE.equalsIgnoreCase(str(b.getOp()))) {
                deleteBankInventoryDocs(b);
                continue;
            }

            List<InventoryTransaction> bankInventory = inventoryByBank.getOrDefault(bankKey, List.of());
            deleteBankInventoryDocs(b);
            if (bankInventory.isEmpty()) {
                continue;
            }

            for (InventoryTransaction txn : bankInventory) {
                String bloodGroup = str(txn.getBloodGroup());
                String component = str(txn.getComponent());
                int unitsAvailable = txn.getRunningBalanceAfter() == null ? 0 : txn.getRunningBalanceAfter();
                String updatedAt = str(txn.getUpdatedAt()).isBlank() ? str(b.getUpdatedAt()) : str(txn.getUpdatedAt());

                Map<String, Object> doc = toBankInventoryDoc(b, bloodGroup, component, unitsAvailable, updatedAt);
                addBulkIndex(bulkLines, Constants.ELASTIC_INDEX_BANKS, bankInventoryDocId(b.getSource(), b.getBankId(), bloodGroup, component), doc);
            }
        }

        flushBulk(bulkLines);

        log.info("api.exit ElasticsearchLoader.loadBanks");
    }

    private Map<String, Object> toBankInventoryDoc(BloodBank bank, String bloodGroup, String component, int unitsAvailable, String updatedAt) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("source", str(bank.getSource()));
        doc.put("blood_bank_id", str(bank.getBankId()));
        doc.put("contact_number", str(bank.getPhone()));
        doc.put("blood_bank_name", str(bank.getBankName()));
        doc.put("source_record_id", str(bank.getBankId()));
        doc.put("event_time", isoDateTime(updatedAt));
        doc.put("address", str(bank.getAddress()));
        doc.put("city", str(bank.getCity()));
        doc.put("state", str(bank.getState()));
        doc.put("pincode", str(bank.getPincode()));
        doc.put("category", str(bank.getCategory()));
        doc.put("email", str(bank.getEmail()));
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("lat", bank.getLat() == null ? 0.0 : bank.getLat());
        location.put("lon", bank.getLon() == null ? 0.0 : bank.getLon());
        doc.put("location", location);
        doc.put("blood_group", bloodGroup);
        doc.put("component", component);
        doc.put("units_available", unitsAvailable);
        return doc;
    }

    public void bootstrap() {
        log.info("api.enter ElasticsearchLoader.bootstrap");
        putTemplate("bb_inventory_current_template", BANK_TEMPLATE);
        putTemplate("donor_availability_current_template", DONOR_TEMPLATE);
        deleteIndex(Constants.ELASTIC_INDEX_BANKS);
        deleteIndex(Constants.ELASTIC_INDEX_DONORS);
        log.info("api.exit ElasticsearchLoader.bootstrap");
    }

    public void loadDonors(List<Donor> donors) {
        log.info("api.enter ElasticsearchLoader.loadDonors");
        if (donors == null || donors.isEmpty()) {
            log.info("api.exit ElasticsearchLoader.loadDonors");
            return;
        }

        List<String> bulkLines = new ArrayList<>();
        for (Donor d : donors) {
            String id = sourceAwareId(d.getSource(), d.getDonorId());
            if (Constants.OP_DELETE.equalsIgnoreCase(str(d.getOp()))) {
                addBulkDelete(bulkLines, Constants.ELASTIC_INDEX_DONORS, id);
            } else {
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("source", str(d.getSource()));
                doc.put("source_record_id", str(d.getDonorId()));
                doc.put("name", str(d.getName()));
                doc.put("contact_number", str(d.getPhone()));
                doc.put("event_time", isoDateTime(str(d.getUpdatedAt())));
                doc.put("blood_group", str(d.getBloodGroup()));
                doc.put("pincode", str(d.getPincodeCurrent()));
                doc.put("city", str(d.getCityCurrent()));
                doc.put("state", str(d.getStateCurrent()));
                Map<String, Object> location = new LinkedHashMap<>();
                location.put("lat", d.getLat() == null ? 0.0 : d.getLat());
                location.put("lon", d.getLon() == null ? 0.0 : d.getLon());
                doc.put("location", location);
                doc.put("last_donated_at", str(d.getLastDonatedOn()));
                doc.put("availability_status", true);

                addBulkIndex(bulkLines, Constants.ELASTIC_INDEX_DONORS, id, doc);
            }
        }

        flushBulk(bulkLines);

        log.info("api.exit ElasticsearchLoader.loadDonors");
    }

    private void deleteBankInventoryDocs(BloodBank bank) {
        String body = "{"
            + "\"query\":{"
            + "\"bool\":{"
            + "\"filter\":["
            + "{\"term\":{\"source\":\"" + escJson(str(bank.getSource())) + "\"}},"
            + "{\"term\":{\"blood_bank_id\":\"" + escJson(str(bank.getBankId())) + "\"}}"
            + "]}}}";

        try {
            elastic.post()
                .uri("/{index}/_delete_by_query?refresh=true", Constants.ELASTIC_INDEX_BANKS)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .body(Objects.requireNonNull(body))
                .retrieve()
                .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound ignored) {
            log.debug("Skipping bank inventory cleanup because {} does not exist yet", Constants.ELASTIC_INDEX_BANKS);
        }
    }

    private void putTemplate(String templateName, String body) {
        elastic.put()
            .uri("/_index_template/{name}", templateName)
            .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
            .body(Objects.requireNonNull(body))
            .retrieve()
            .toBodilessEntity();
    }

    private void addBulkIndex(List<String> bulkLines, String indexName, String id, Map<String, Object> doc) {
        bulkLines.add("{\"index\":{\"_index\":\"" + escJson(indexName) + "\",\"_id\":\"" + escJson(id) + "\"}}");
        bulkLines.add(Objects.requireNonNull(jsonUtil.toJson(doc)));
    }

    private void addBulkDelete(List<String> bulkLines, String indexName, String id) {
        bulkLines.add("{\"delete\":{\"_index\":\"" + escJson(indexName) + "\",\"_id\":\"" + escJson(id) + "\"}}");
    }

    private void flushBulk(List<String> bulkLines) {
        if (bulkLines.isEmpty()) {
            return;
        }

        for (int start = 0; start < bulkLines.size(); start += BULK_BATCH_SIZE * 2) {
            int end = Math.min(start + BULK_BATCH_SIZE * 2, bulkLines.size());
            StringBuilder body = new StringBuilder();
            for (String line : bulkLines.subList(start, end)) {
                body.append(line).append('\n');
            }

            String response = elastic.post()
                .uri("/_bulk")
                .contentType(NDJSON)
                .body(body.toString())
                .retrieve()
                .body(String.class);

            if (response != null && response.contains("\"errors\":true")) {
                throw new RuntimeException("elasticsearch bulk write failed: " + response);
            }
        }
    }

    private void deleteIndex(String indexName) {
        try {
            elastic.delete().uri("/{index}", indexName).retrieve().toBodilessEntity();
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() != 404) {
                throw e;
            }
        }
    }

}
