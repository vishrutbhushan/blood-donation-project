package com.redcross.backend.controller;

import com.redcross.backend.repository.RedcrossRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedcrossController {

    private final RedcrossRepository repo;

    public RedcrossController(RedcrossRepository repo) {
        this.repo = repo;
    }

    // ─── api_contracts.txt endpoints ──────────────────────────────────────────

    @GetMapping("/api/redcross/centres")
    public List<Map<String, Object>> getCentres() {
        return repo.fetchAllCentres();
    }

    @GetMapping("/api/redcross/centres/incremental")
    public List<Map<String, Object>> getCentresIncremental(@RequestParam long since) {
        return repo.fetchCentresSince(since);
    }

    @GetMapping("/api/redcross/people")
    public List<Map<String, Object>> getPeople() {
        return repo.fetchAllPeople();
    }

    @GetMapping("/api/redcross/people/incremental")
    public List<Map<String, Object>> getPeopleIncremental(@RequestParam long since) {
        return repo.fetchPeopleSince(since);
    }

    // ─── ETL combined endpoint ─────────────────────────────────────────────────
    // ETL calls: GET /incremental?since={epoch_ms}&until={epoch_ms}
    // Returns:   { "centres": [...banks...], "people": [...donors...] }

    @GetMapping("/incremental")
    public Map<String, Object> etlIncremental(
            @RequestParam long since,
            @RequestParam long until) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("centres", repo.fetchEtlBanks(since, until));
        result.put("people", repo.fetchEtlDonors(since, until));
        return result;
    }
}
