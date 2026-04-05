package com.who.backend.controller;

import com.who.backend.repository.WhoRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhoController {

    private final WhoRepository repo;

    public WhoController(WhoRepository repo) {
        this.repo = repo;
    }

    // ─── api_contracts.txt endpoints ──────────────────────────────────────────

    @GetMapping("/api/who/blood-banks")
    public List<Map<String, Object>> getBloodBanks() {
        return repo.fetchAllBloodBanks();
    }

    @GetMapping("/api/who/blood-banks/incremental")
    public List<Map<String, Object>> getBloodBanksIncremental(@RequestParam long since) {
        return repo.fetchBloodBanksSince(since);
    }

    @GetMapping("/api/who/donors")
    public List<Map<String, Object>> getDonors() {
        return repo.fetchAllDonors();
    }

    @GetMapping("/api/who/donors/incremental")
    public List<Map<String, Object>> getDonorsIncremental(@RequestParam long since) {
        return repo.fetchDonorsSince(since);
    }

    // ─── ETL combined endpoint ─────────────────────────────────────────────────
    // ETL calls: GET /incremental?since={epoch_ms}&until={epoch_ms}
    // Returns:   { "blood_banks": [...banks...], "donors": [...donors...] }

    @GetMapping("/incremental")
    public Map<String, Object> etlIncremental(
            @RequestParam long since,
            @RequestParam long until) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("blood_banks", repo.fetchEtlBanks(since, until));
        result.put("donors", repo.fetchEtlDonors(since, until));
        return result;
    }
}
