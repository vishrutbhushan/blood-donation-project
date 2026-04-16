package com.redcross.backend.controller;

import com.redcross.backend.dto.RedcrossCentreDTO;
import com.redcross.backend.dto.RedcrossDonorDTO;
import com.redcross.backend.dto.RedcrossIncrementalResponseDTO;
import com.redcross.backend.repository.RedcrossRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class RedcrossController {

    private final RedcrossRepository repo;

    public RedcrossController(RedcrossRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/redcross/centres")
    public List<RedcrossCentreDTO> getCentres() {
        log.info("api.enter redcross.centres");
        List<RedcrossCentreDTO> centres = repo.fetchAllCentres();
        log.info("api.exit redcross.centres count={}", centres.size());
        return centres;
    }

    @GetMapping("/api/redcross/centres/incremental")
    public List<RedcrossCentreDTO> getCentresIncremental(@RequestParam long since) {
        log.info("api.enter redcross.centres.incremental since={}", since);
        List<RedcrossCentreDTO> centres = repo.fetchCentresSince(since);
        log.info("api.exit redcross.centres.incremental count={}", centres.size());
        return centres;
    }

    @GetMapping("/api/redcross/people")
    public List<RedcrossDonorDTO> getPeople(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String pincode,
            @RequestParam(defaultValue = "200") int limit) {
        log.info("api.enter redcross.people bloodGroup={} pincode={} limit={}", bloodGroup, pincode, limit);
        List<RedcrossDonorDTO> donors = repo.fetchPeopleFiltered(bloodGroup, pincode, limit);
        log.info("api.exit redcross.people count={}", donors.size());
        return donors;
    }

    @GetMapping("/api/redcross/people/incremental")
    public List<RedcrossDonorDTO> getPeopleIncremental(@RequestParam long since) {
        log.info("api.enter redcross.people.incremental since={}", since);
        List<RedcrossDonorDTO> donors = repo.fetchPeopleSince(since);
        log.info("api.exit redcross.people.incremental count={}", donors.size());
        return donors;
    }

    @GetMapping("/incremental")
    public RedcrossIncrementalResponseDTO etlIncremental(
            @RequestParam long since,
            @RequestParam long until) {
        log.info("api.enter redcross.incremental since={} until={}", since, until);
        RedcrossIncrementalResponseDTO response = new RedcrossIncrementalResponseDTO(
            repo.fetchEtlBanks(since, until),
            repo.fetchEtlDonors(since, until),
            repo.fetchEtlInventoryTransactions(since, until));
        log.info("api.exit redcross.incremental centres={} people={} inventoryTxns={}",
            response.getCentres().size(),
            response.getPeople().size(),
            response.getInventory_transactions().size());
        return response;
    }
}
