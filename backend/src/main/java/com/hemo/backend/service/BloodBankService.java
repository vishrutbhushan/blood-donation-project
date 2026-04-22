package com.hemo.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hemo.backend.dto.BloodBankDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BloodBankService {
    private static final int MAX_RESULTS = 20;
    private static final int FETCH_SIZE = 200;
        private static final String SEARCH_QUERY_TEMPLATE = """
                        {
                            "size": %d,
                            "track_total_hits": false,
                            "query": {
                                "bool": {
                                    "filter": [
                                        %s
                                    ],
                                    "must_not": [
                                        { "geo_distance": { "distance": "1m", "location": { "lat": 0, "lon": 0 } } }
                                    ]
                                }
                            },
                            "sort": [
                                {
                                    "_geo_distance": {
                                        "location": { "lat": %.8f, "lon": %.8f },
                                        "order": "asc",
                                        "unit": "km",
                                        "distance_type": "arc"
                                    }
                                }
                            ]
                        }
                        """;

    private final RestClient elasticsearchClient;
    private final BloodCompatibilityService bloodCompatibilityService;

    public BloodBankService(
            @Qualifier("elasticsearchRestClient") RestClient elasticsearchClient,
            BloodCompatibilityService bloodCompatibilityService) {
        this.elasticsearchClient = elasticsearchClient;
        this.bloodCompatibilityService = bloodCompatibilityService;
    }

    public List<BloodBankDTO> findNearestBloodBanks(Double userLatitude, Double userLongitude, String bloodGroup, String component) {
        try {
            String queryBody = buildQueryBody(userLatitude, userLongitude, bloodGroup, component);
            EsSearchResponse response = elasticsearchClient.post()
                .uri("/bb_inventory_current/_search")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .body(Objects.requireNonNull(queryBody))
                .retrieve()
                .body(EsSearchResponse.class);

            return toDtos(response);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String buildQueryBody(Double userLatitude, Double userLongitude, String bloodGroup, String component) {
        List<String> filters = new ArrayList<>();
        filters.add("{ \"exists\": { \"field\": \"location\" } }");

        if (bloodGroup != null && !bloodGroup.isBlank()) {
            List<String> compatibleGroups = bloodCompatibilityService.compatibleDonorGroups(bloodGroup);
            if (!compatibleGroups.isEmpty()) {
                filters.add("{ \"terms\": { \"blood_group\": [" + joinQuotedJsonValues(compatibleGroups) + "] } }");
            }
        }

        if (component != null && !component.isBlank()) {
            String normalizedComponent = escapeJson(component.trim());
            filters.add("{ \"bool\": { \"should\": ["
                + "{ \"term\": { \"component\": \"" + normalizedComponent + "\" } },"
                + "{ \"term\": { \"component_type\": \"" + normalizedComponent + "\" } },"
                + "{ \"term\": { \"blood_component\": \"" + normalizedComponent + "\" } }"
                + "], \"minimum_should_match\": 1 } }");
        }

        return String.format(Locale.US, SEARCH_QUERY_TEMPLATE, FETCH_SIZE, String.join(",\n                    ", filters), userLatitude, userLongitude);
    }

    private String joinQuotedJsonValues(List<String> values) {
        List<String> quoted = new ArrayList<>();
        for (String value : values) {
            quoted.add("\"" + escapeJson(value) + "\"");
        }
        return String.join(", ", quoted);
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private List<BloodBankDTO> toDtos(EsSearchResponse response) {
        Map<String, BloodBankDTO> banks = new LinkedHashMap<>();
        if (response == null || response.getHits() == null || response.getHits().getHits() == null) {
            return new ArrayList<>();
        }

        for (EsHit row : response.getHits().getHits()) {
            EsBankSource source = row.get_source();
            if (source == null) {
                continue;
            }

            String bankKey = emptyToBlank(source.getSource()) + ":" + emptyToBlank(source.getBlood_bank_id() == null ? null : String.valueOf(source.getBlood_bank_id()));
            BloodBankDTO bank = banks.get(bankKey);
            if (bank == null) {
                bank = BloodBankDTO.builder()
                    .bankId(source.getBlood_bank_id())
                    .sourceId(emptyToBlank(source.getSource()))
                    .sourceBankId(emptyToBlank(source.getSource_record_id()))
                    .bankName(emptyToBlank(source.getBlood_bank_name()))
                    .category(emptyToBlank(source.getCategory()))
                    .phone(emptyToBlank(source.getContact_number()))
                    .email(emptyToDefault(source.getEmail(), "N/A"))
                    .pincode(emptyToBlank(source.getPincode()))
                    .city(emptyToBlank(source.getCity()))
                    .state(emptyToBlank(source.getState()))
                    .address(emptyToBlank(source.getAddress()))
                    .latitude(source.getLocation() == null || source.getLocation().getLat() == null ? 0.0 : source.getLocation().getLat())
                    .longitude(source.getLocation() == null || source.getLocation().getLon() == null ? 0.0 : source.getLocation().getLon())
                    .distanceKm(row.getSort() == null || row.getSort().isEmpty() ? 0.0 : row.getSort().get(0))
                    .compatibleStock(new ArrayList<>())
                    .build();
                banks.put(bankKey, bank);
            }

            if (source.getBlood_group() != null && source.getComponent() != null) {
                bank.getCompatibleStock().add(BloodBankDTO.CompatibleStockDTO.builder()
                    .bloodGroup(emptyToBlank(source.getBlood_group()))
                    .component(emptyToBlank(source.getComponent()))
                    .unitsAvailable(source.getUnits_available() == null ? 0 : source.getUnits_available())
                    .build());
            }
        }

        return new ArrayList<>(banks.values()).subList(0, Math.min(MAX_RESULTS, banks.size()));
    }

    private String emptyToBlank(String value) {
        return value == null ? "" : value;
    }

    private String emptyToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EsSearchResponse {
        private EsHits hits;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EsHits {
        private List<EsHit> hits;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EsHit {
        @JsonProperty("_source")
        private EsBankSource _source;
        private List<Double> sort;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EsBankSource {
        private Long blood_bank_id;
        private String source;
        private String source_record_id;
        private String blood_bank_name;
        private String category;
        private String contact_number;
        private String email;
        private String pincode;
        private String city;
        private String state;
        private String address;
        private EsLocation location;
        @JsonProperty("blood_group")
        private String blood_group;
        private String component;
        @JsonProperty("units_available")
        private Integer units_available;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EsLocation {
        private Double lat;
        private Double lon;
    }
}
