package com.redcross.backend.controller;

import com.redcross.backend.dto.RedcrossCentreDTO;
import com.redcross.backend.dto.RedcrossDonorDTO;
import com.redcross.backend.dto.RedcrossIncrementalResponseDTO;
import com.redcross.backend.repository.RedcrossRepository;
import java.util.List;
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
    public List<RedcrossCentreDTO> getCentres() {
        return repo.fetchAllCentres();
    }

    @GetMapping("/api/redcross/centres/incremental")
    public List<RedcrossCentreDTO> getCentresIncremental(@RequestParam long since) {
        return repo.fetchCentresSince(since);
    }

    @GetMapping("/api/redcross/people")
    public List<RedcrossDonorDTO> getPeople(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String pincode,
            @RequestParam(defaultValue = "200") int limit) {
        return repo.fetchPeopleFiltered(bloodGroup, pincode, limit);
    }

    @GetMapping("/api/redcross/people/incremental")
    public List<RedcrossDonorDTO> getPeopleIncremental(@RequestParam long since) {
        return repo.fetchPeopleSince(since);
    }

    // ─── ETL combined endpoint ─────────────────────────────────────────────────
    // ETL calls: GET /incremental?since={epoch_ms}&until={epoch_ms}
    // Returns:   { "centres": [...banks...], "people": [...donors...] }

    @GetMapping("/incremental")
    public RedcrossIncrementalResponseDTO etlIncremental(
            @RequestParam long since,
            @RequestParam long until) {
        return new RedcrossIncrementalResponseDTO(repo.fetchEtlBanks(since, until), repo.fetchEtlDonors(since, until));
    }
}
