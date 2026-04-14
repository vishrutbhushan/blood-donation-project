package com.hemo.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemo.backend.dto.DonorCandidateDTO;
import com.hemo.backend.dto.DonorSearchResponseDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DonorSearchService {
        private static final String QUERY_WITH_GEO_TEMPLATE = """
                {
                    "from": %d,
                    "size": %d,
                    "track_total_hits": true,
                    "query": {
                        "bool": {
                            "filter": [
                                { "terms": { "blood_group": [%s] } },
                                { "term": { "availability_status": true } },
                                { "exists": { "field": "location" } }
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
                        },
                        { "event_time": { "order": "desc" } }
                    ]
                }
                """;

        private static final String QUERY_NO_GEO_TEMPLATE = """
                {
                    "from": %d,
                    "size": %d,
                    "track_total_hits": true,
                    "query": {
                        "bool": {
                            "filter": [
                                { "terms": { "blood_group": [%s] } },
                                { "term": { "availability_status": true } },
                                { "exists": { "field": "location" } }
                            ]
                        }
                    },
                    "sort": [
                        { "event_time": { "order": "desc" } }
                    ]
                }
                """;

    private final ObjectMapper objectMapper;
    private final BloodCompatibilityService bloodCompatibilityService;
    private final PincodeGeoService pincodeGeoService;
    private final RestClient elasticsearchClient;

    public DonorSearchService(
            ObjectMapper objectMapper,
            BloodCompatibilityService bloodCompatibilityService,
            PincodeGeoService pincodeGeoService,
            @Qualifier("elasticsearchRestClient") RestClient elasticsearchClient) {
        this.objectMapper = objectMapper;
        this.bloodCompatibilityService = bloodCompatibilityService;
        this.pincodeGeoService = pincodeGeoService;
        this.elasticsearchClient = elasticsearchClient;
    }

    public DonorSearchResponseDTO searchCompatibleDonors(String recipientBloodGroup, String pincode, int offset, int limit) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, Math.min(limit, 200));
        String normalizedRecipient = bloodCompatibilityService.normalize(recipientBloodGroup);
        List<String> compatibleGroups = bloodCompatibilityService.compatibleDonorGroups(normalizedRecipient);
        PincodeGeoService.GeoPoint geoPoint = pincodeGeoService.resolve(pincode).orElse(null);

        try {
            String body = buildQuery(compatibleGroups, geoPoint, safeOffset, safeLimit);
            String json = elasticsearchClient.post()
                .uri("/donor_availability_current/_search")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(json == null ? "{}" : json);
            JsonNode hitsNode = root.path("hits");
            long total = hitsNode.path("total").path("value").asLong(0L);
            JsonNode rows = hitsNode.path("hits");
            List<DonorCandidateDTO> donors = new ArrayList<>();

            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    JsonNode src = row.path("_source");
                    String city = src.path("city").asText("");
                    String state = src.path("state").asText("");
                    String location = city.isBlank() ? state : (state.isBlank() ? city : city + ", " + state);
                    double distance = 0.0;
                    JsonNode sort = row.path("sort");
                    if (sort.isArray() && sort.size() > 0) {
                        distance = sort.get(0).asDouble(0.0);
                    }

                    donors.add(DonorCandidateDTO.builder()
                        .donorId(src.path("source").asText("") + ":" + src.path("source_record_id").asText(""))
                        .name(src.path("name").asText("Unknown"))
                        .bloodGroup(src.path("blood_group").asText(""))
                        .phone(src.path("contact_number").asText(""))
                        .pincode(src.path("pincode").asText(""))
                        .location(location)
                        .source(src.path("source").asText(""))
                        .distanceKm(distance)
                        .build());
                }
            }

            return DonorSearchResponseDTO.builder()
                .recipientBloodGroup(normalizedRecipient)
                .compatibleDonorGroups(compatibleGroups)
                .totalMatched(total)
                .offset(safeOffset)
                .limit(safeLimit)
                .donors(donors)
                .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to query donors from Elasticsearch", ex);
        }
    }

    private String buildQuery(List<String> compatibleGroups, PincodeGeoService.GeoPoint geoPoint, int offset, int limit) {
        String terms = compatibleGroups.stream()
            .map(v -> "\"" + escape(v) + "\"")
            .collect(Collectors.joining(","));
        if (geoPoint != null) {
            return String.format(
                Locale.US,
                QUERY_WITH_GEO_TEMPLATE,
                offset,
                limit,
                terms,
                geoPoint.lat(),
                geoPoint.lon()
            );
        }
        return String.format(Locale.US, QUERY_NO_GEO_TEMPLATE, offset, limit, terms);
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
