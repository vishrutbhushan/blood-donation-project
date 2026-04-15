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
        List<RedcrossCentreDTO> centres = repo.fetchAllCentres();
        log.info("redcross centres fetched count={}", centres.size());
        return centres;
    }

    @GetMapping("/api/redcross/centres/incremental")
    public List<RedcrossCentreDTO> getCentresIncremental(@RequestParam long since) {
        List<RedcrossCentreDTO> centres = repo.fetchCentresSince(since);
        log.info("redcross centres incremental since={} count={}", since, centres.size());
        return centres;
    }

    @GetMapping("/api/redcross/people")
    public List<RedcrossDonorDTO> getPeople(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String pincode,
            @RequestParam(defaultValue = "200") int limit) {
        List<RedcrossDonorDTO> donors = repo.fetchPeopleFiltered(bloodGroup, pincode, limit);
        log.info("redcross people fetched bloodGroup={} pincode={} limit={} count={}", bloodGroup, pincode, limit, donors.size());
        return donors;
    }

    @GetMapping("/api/redcross/people/incremental")
    public List<RedcrossDonorDTO> getPeopleIncremental(@RequestParam long since) {
        List<RedcrossDonorDTO> donors = repo.fetchPeopleSince(since);
        log.info("redcross people incremental since={} count={}", since, donors.size());
        return donors;
    }

    @GetMapping("/incremental")
    public RedcrossIncrementalResponseDTO etlIncremental(
            @RequestParam long since,
            @RequestParam long until) {
        RedcrossIncrementalResponseDTO response = new RedcrossIncrementalResponseDTO(repo.fetchEtlBanks(since, until), repo.fetchEtlDonors(since, until));
        log.info("redcross etl incremental since={} until={} centres={} people={}", since, until, response.getCentres().size(), response.getPeople().size());
        return response;
    }
}
