package com.hemo.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemo.backend.dto.DonorCandidateDTO;
import com.hemo.backend.dto.DonorSearchResponseDTO;
import com.hemo.backend.dto.DonorSearchSummaryDTO;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DonorSearchService {
    private static final int MAX_LIMIT = 20;

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
        return searchCompatibleDonors(recipientBloodGroup, pincode, offset, limit, Collections.emptyList());
    }

    public DonorSearchResponseDTO searchCompatibleDonors(
            String recipientBloodGroup,
            String pincode,
            int offset,
            int limit,
            List<String> excludedDonorIds) {
        int safeOffset = Math.max(0, offset);
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        String normalizedRecipient = bloodCompatibilityService.normalize(recipientBloodGroup);
        List<String> compatibleGroups = bloodCompatibilityService.compatibleDonorGroups(normalizedRecipient);
        PincodeGeoService.GeoPoint geoPoint = pincodeGeoService.resolve(pincode).orElse(null);
        Set<String> excluded = excludedDonorIds == null ? Collections.emptySet() : new HashSet<>(excludedDonorIds);

        try {
            String body = buildElasticsearchQuery(compatibleGroups, geoPoint, safeOffset, safeLimit);
            String json = elasticsearchClient.post()
                .uri("/donor_availability_current/_search")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .body(Objects.requireNonNull(body))
                .retrieve()
                .body(String.class);

            JsonNode root = objectMapper.readTree(json == null ? "{}" : json);
            JsonNode hitsNode = root.path("hits");
            JsonNode rows = hitsNode.path("hits");
            List<DonorCandidateDTO> donors = new ArrayList<>();
            long below10 = 0;
            long below50 = 0;
            long above50 = 0;

            if (rows.isArray()) {
                for (JsonNode row : rows) {
                    JsonNode src = row.path("_source");
                    String donorId = src.path("source").asText("") + ":" + src.path("source_record_id").asText("");
                    if (excluded.contains(donorId)) {
                        continue;
                    }
                    String city = src.path("city").asText("");
                    String state = src.path("state").asText("");
                    String location = city.isBlank() ? state : (state.isBlank() ? city : city + ", " + state);
                    double distance = 0.0;
                    JsonNode sort = row.path("sort");
                    if (sort.isArray() && sort.size() > 0) {
                        distance = sort.get(0).asDouble(0.0);
                    }

                    if (distance < 10.0) {
                        below10++;
                    } else if (distance < 50.0) {
                        below50++;
                    } else {
                        above50++;
                    }

                    donors.add(DonorCandidateDTO.builder()
                        .donorId(donorId)
                        .abhaId(resolveAbhaId(src, donorId))
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

            long bucketedTotal = below10 + below50 + above50;
            return DonorSearchResponseDTO.builder()
                .recipientBloodGroup(normalizedRecipient)
                .compatibleDonorGroups(compatibleGroups)
                .totalMatched(bucketedTotal)
                .below10KmCount(below10)
                .below50KmCount(below50)
                .above50KmCount(above50)
                .offset(safeOffset)
                .limit(safeLimit)
                .donors(donors)
                .build();
        } catch (Exception ex) {
            return DonorSearchResponseDTO.builder()
                .recipientBloodGroup(normalizedRecipient)
                .compatibleDonorGroups(compatibleGroups)
                .totalMatched(0L)
                .below10KmCount(0L)
                .below50KmCount(0L)
                .above50KmCount(0L)
                .offset(safeOffset)
                .limit(safeLimit)
                .donors(List.of())
                .build();
        }
    }

    private String buildElasticsearchQuery(List<String> compatibleGroups, PincodeGeoService.GeoPoint geoPoint, int offset, int limit) {
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

    private String resolveAbhaId(JsonNode src, String donorId) {
        String abhaHash = src.path("abha_hash").asText("").trim();
        if (!abhaHash.isBlank()) {
            return abhaHash;
        }

        String abhaId = src.path("abha_id").asText("").trim();
        if (!abhaId.isBlank()) {
            return abhaId;
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(donorId.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            BigInteger number = new BigInteger(1, bytes).mod(BigInteger.TEN.pow(14));
            return String.format("%014d", number.longValue());
        } catch (NoSuchAlgorithmException ex) {
            long fallback = Math.floorMod(donorId.hashCode(), 100000000000000L);
            return String.format("%014d", fallback);
        }
    }

    public DonorSearchSummaryDTO toSummaryDTO(DonorSearchResponseDTO response) {
        long baseTotal = response.getTotalMatched() == null ? 0L : response.getTotalMatched();
        long baseBelow10 = response.getBelow10KmCount() == null ? 0L : response.getBelow10KmCount();
        long baseBelow50 = response.getBelow50KmCount() == null ? 0L : response.getBelow50KmCount();
        long baseAbove50 = response.getAbove50KmCount() == null ? 0L : response.getAbove50KmCount();

        return DonorSearchSummaryDTO.builder()
            .recipientBloodGroup(response.getRecipientBloodGroup())
            .compatibleDonorGroups(response.getCompatibleDonorGroups())
            .totalMatched(baseTotal)
            .below10KmCount(baseBelow10)
            .below50KmCount(baseBelow50)
            .above50KmCount(baseAbove50)
            .offset(response.getOffset())
            .limit(response.getLimit())
            .build();
    }
}
