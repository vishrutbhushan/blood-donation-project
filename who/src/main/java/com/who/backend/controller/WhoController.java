package com.who.backend.controller;

import com.who.backend.dto.WhoBloodBankDTO;
import com.who.backend.dto.WhoDonorDTO;
import com.who.backend.dto.WhoIncrementalResponseDTO;
import com.who.backend.repository.WhoRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class WhoController {

    private final WhoRepository repo;

    public WhoController(WhoRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/api/who/blood-banks")
    public List<WhoBloodBankDTO> getBloodBanks() {
        List<WhoBloodBankDTO> banks = repo.fetchAllBloodBanks();
        log.info("who blood-banks fetched count={}", banks.size());
        return banks;
    }

    @GetMapping("/api/who/blood-banks/incremental")
    public List<WhoBloodBankDTO> getBloodBanksIncremental(@RequestParam long since) {
        List<WhoBloodBankDTO> banks = repo.fetchBloodBanksSince(since);
        log.info("who blood-banks incremental since={} count={}", since, banks.size());
        return banks;
    }

    @GetMapping("/api/who/donors")
    public List<WhoDonorDTO> getDonors(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String pincode,
            @RequestParam(defaultValue = "200") int limit) {
        List<WhoDonorDTO> donors = repo.fetchDonorsFiltered(bloodGroup, pincode, limit);
        log.info("who donors fetched bloodGroup={} pincode={} limit={} count={}", bloodGroup, pincode, limit, donors.size());
        return donors;
    }

    @GetMapping("/api/who/donors/incremental")
    public List<WhoDonorDTO> getDonorsIncremental(@RequestParam long since) {
        List<WhoDonorDTO> donors = repo.fetchDonorsSince(since);
        log.info("who donors incremental since={} count={}", since, donors.size());
        return donors;
    }

    @GetMapping("/incremental")
    public WhoIncrementalResponseDTO etlIncremental(
            @RequestParam long since,
            @RequestParam long until) {
        WhoIncrementalResponseDTO response = new WhoIncrementalResponseDTO(repo.fetchEtlBanks(since, until), repo.fetchEtlDonors(since, until));
        log.info("who etl incremental since={} until={} banks={} donors={}", since, until, response.getBlood_banks().size(), response.getDonors().size());
        return response;
    }
}
