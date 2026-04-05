package com.G9.hemoconnect.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.G9.hemoconnect.dto.SearchRequest;
import com.G9.hemoconnect.entity.Search;
import com.G9.hemoconnect.repository.SearchRepository;
import com.G9.hemoconnect.util.PincodeCoordinates;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SearchController {
    private final SearchRepository searchRepo;
    private final ElasticsearchClient esClient;
    private final PincodeCoordinates pincodeCoords;

    @PostMapping("/search")
    public ResponseEntity<?> createSearch(@RequestBody SearchRequest req) {
        Search search = new Search();
        search.setPatientName(req.getPatientName());
        search.setBloodGroup(req.getBloodGroup());
        search.setComponent(req.getComponent());
        search.setHospitalName(req.getHospitalName());
        search.setHospitalPincode(req.getHospitalPincode());
        search.setCreatedAt(LocalDateTime.now());
        
        Search saved = searchRepo.save(search);
        
        return ResponseEntity.ok(Map.of("searchId", saved.getId()));
    }

    @GetMapping("/bloodbanks/nearby")
    public ResponseEntity<?> searchBloodBanks(
        @RequestParam Long searchId,
        @RequestParam(defaultValue = "5") int radiusKm
    ) throws IOException {
        Search search = searchRepo.findById(searchId).orElseThrow();
        double[] coords = pincodeCoords.getCoords(search.getHospitalPincode());

        SearchResponse<Map> response = esClient.search(s -> s
            .index("bb_inventory_current*")
            .query(q -> q.bool(b -> b
                .must(m -> m.term(t -> t.field("blood_group").value(search.getBloodGroup())))
                .must(m -> m.term(t -> t.field("component").value(search.getComponent())))
                .filter(f -> f.geoDistance(g -> g
                    .field("location")
                    .location(l -> l.latlon(ll -> ll.lat(coords[0]).lon(coords[1])))
                    .distance(radiusKm + "km")
                ))
            ))
            .sort(so -> so.geoDistance(g -> g
                .field("location")
                .location(l -> l.latlon(ll -> ll.lat(coords[0]).lon(coords[1])))
                .order(SortOrder.Asc)
            ))
            .size(20),
            Map.class
        );

        List<Map> results = response.hits().hits().stream()
            .map(Hit::source)
            .collect(Collectors.toList());

        boolean lowStock = results.isEmpty() ||
            results.stream().allMatch(r -> ((Integer) r.get("units_available")) == 0);

        return ResponseEntity.ok(Map.of(
            "bloodBanks", results,
            "lowStock", lowStock
        ));
    }
}
