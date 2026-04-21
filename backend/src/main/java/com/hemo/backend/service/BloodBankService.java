package com.hemo.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.hemo.backend.dto.BloodBankDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BloodBankService {
    private static final int MAX_RESULTS = 20;

    private final RestClient elasticsearchClient;

        public BloodBankService(@Qualifier("elasticsearchRestClient") RestClient elasticsearchClient) {
        this.elasticsearchClient = elasticsearchClient;
    }

    public List<BloodBankDTO> findNearestBloodBanks(Double userLatitude, Double userLongitude) {
        return findNearestBloodBanks(userLatitude, userLongitude, null, null);
    }

    public List<BloodBankDTO> findNearestBloodBanks(Double userLatitude, Double userLongitude, String bloodGroup, String component) {
        try {
            Map<String, Object> queryBody = buildQueryBody(userLatitude, userLongitude, bloodGroup, component);
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

    private Map<String, Object> buildQueryBody(Double userLatitude, Double userLongitude, String bloodGroup, String component) {
        List<Object> filters = new ArrayList<>();
        filters.add(Map.of("exists", Map.of("field", "location")));

        if (bloodGroup != null && !bloodGroup.isBlank()) {
            filters.add(Map.of("term", Map.of("blood_group", bloodGroup.trim())));
        }

        if (component != null && !component.isBlank()) {
            String normalizedComponent = component.trim();
            filters.add(Map.of(
                "bool",
                Map.of(
                    "should",
                    List.of(
                        Map.of("term", Map.of("component", normalizedComponent)),
                        Map.of("term", Map.of("component_type", normalizedComponent)),
                        Map.of("term", Map.of("blood_component", normalizedComponent))
                    ),
                    "minimum_should_match",
                    1
                )
            ));
        }

        Map<String, Object> query = new LinkedHashMap<>();
        query.put("size", MAX_RESULTS);
        query.put("track_total_hits", false);
        query.put("query", Map.of(
            "bool",
            Map.of(
                "filter", filters,
                "must_not", List.of(Map.of("geo_distance", Map.of("distance", "1m", "location", Map.of("lat", 0, "lon", 0))))
            )
        ));
        query.put("sort", List.of(Map.of(
            "_geo_distance",
            Map.of(
                "location", Map.of("lat", userLatitude, "lon", userLongitude),
                "order", "asc",
                "unit", "km",
                "distance_type", "arc"
            )
        )));
        return query;
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
