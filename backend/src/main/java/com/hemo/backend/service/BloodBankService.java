package com.hemo.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hemo.backend.dto.BloodBankDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BloodBankService {
        private static final String NEAREST_BANKS_QUERY_TEMPLATE = """
                        {
                            "size": 20,
                            "track_total_hits": false,
                            "query": {
                                "bool": {
                                    "filter": [
                                                                                { "exists": { "field": "location" } }
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

        public BloodBankService(@Qualifier("elasticsearchRestClient") RestClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public List<BloodBankDTO> findNearestBloodBanks(Double userLatitude, Double userLongitude) {
        return findNearestBloodBanks(userLatitude, userLongitude, null, null);
    }

    public List<BloodBankDTO> findNearestBloodBanks(Double userLatitude, Double userLongitude, String bloodGroup, String component) {
        try {
            String optionalFilters = buildOptionalFilters(bloodGroup, component);
            String queryBody = String.format(Locale.US, NEAREST_BANKS_QUERY_TEMPLATE, optionalFilters, userLatitude, userLongitude);
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

    private List<BloodBankDTO> toDtos(EsSearchResponse response) {
        List<BloodBankDTO> banks = new ArrayList<>();
        if (response == null || response.getHits() == null || response.getHits().getHits() == null) {
            return banks;
        }

        for (EsHit row : response.getHits().getHits()) {
            EsBankSource source = row.get_source();
            if (source == null) {
                continue;
            }
            BloodBankDTO bank = BloodBankDTO.builder()
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
                .build();

            banks.add(bank);
        }

        return banks;
    }

    private String emptyToBlank(String value) {
        return value == null ? "" : value;
    }

    private String buildOptionalFilters(String bloodGroup, String component) {
        StringBuilder filters = new StringBuilder();
        if (bloodGroup != null && !bloodGroup.isBlank()) {
            filters.append(",\n                    { \"term\": { \"blood_group\": \"")
                    .append(escape(bloodGroup.trim()))
                    .append("\" } }");
        }
        if (component != null && !component.isBlank()) {
            String escapedComponent = escape(component.trim());
            filters.append(",\n                    { \"bool\": { \"should\": [")
                    .append("{ \"term\": { \"component\": \"").append(escapedComponent).append("\" } },")
                    .append("{ \"term\": { \"component_type\": \"").append(escapedComponent).append("\" } },")
                    .append("{ \"term\": { \"blood_component\": \"").append(escapedComponent).append("\" } }")
                    .append("], \"minimum_should_match\": 1 } }");
        }
        return filters.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EsLocation {
        private Double lat;
        private Double lon;
    }
}
