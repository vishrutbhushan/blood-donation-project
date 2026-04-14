package com.who.backend.controller;

import com.who.backend.dto.WhoBloodBankDTO;
import com.who.backend.dto.WhoDonorDTO;
import com.who.backend.dto.WhoIncrementalResponseDTO;
import com.who.backend.repository.WhoRepository;
import java.util.List;
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
    public List<WhoBloodBankDTO> getBloodBanks() {
        return repo.fetchAllBloodBanks();
    }

    @GetMapping("/api/who/blood-banks/incremental")
    public List<WhoBloodBankDTO> getBloodBanksIncremental(@RequestParam long since) {
        return repo.fetchBloodBanksSince(since);
    }

    @GetMapping("/api/who/donors")
    public List<WhoDonorDTO> getDonors(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String pincode,
            @RequestParam(defaultValue = "200") int limit) {
        return repo.fetchDonorsFiltered(bloodGroup, pincode, limit);
    }

    @GetMapping("/api/who/donors/incremental")
    public List<WhoDonorDTO> getDonorsIncremental(@RequestParam long since) {
        return repo.fetchDonorsSince(since);
    }

    // ─── ETL combined endpoint ─────────────────────────────────────────────────
    // ETL calls: GET /incremental?since={epoch_ms}&until={epoch_ms}
    // Returns:   { "blood_banks": [...banks...], "donors": [...donors...] }

    @GetMapping("/incremental")
    public WhoIncrementalResponseDTO etlIncremental(
            @RequestParam long since,
            @RequestParam long until) {
        return new WhoIncrementalResponseDTO(repo.fetchEtlBanks(since, until), repo.fetchEtlDonors(since, until));
    }
}
