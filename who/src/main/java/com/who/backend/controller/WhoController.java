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
        log.info("api.enter who.blood-banks");
        List<WhoBloodBankDTO> banks = repo.fetchAllBloodBanks();
        log.info("api.exit who.blood-banks count={}", banks.size());
        return banks;
    }

    @GetMapping("/api/who/blood-banks/incremental")
    public List<WhoBloodBankDTO> getBloodBanksIncremental(@RequestParam long since) {
        log.info("api.enter who.blood-banks.incremental since={}", since);
        List<WhoBloodBankDTO> banks = repo.fetchBloodBanksSince(since);
        log.info("api.exit who.blood-banks.incremental count={}", banks.size());
        return banks;
    }

    @GetMapping("/api/who/donors")
    public List<WhoDonorDTO> getDonors(
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String pincode,
            @RequestParam(defaultValue = "200") int limit) {
        log.info("api.enter who.donors bloodGroup={} pincode={} limit={}", bloodGroup, pincode, limit);
        List<WhoDonorDTO> donors = repo.fetchDonorsFiltered(bloodGroup, pincode, limit);
        log.info("api.exit who.donors count={}", donors.size());
        return donors;
    }

    @GetMapping("/api/who/donors/incremental")
    public List<WhoDonorDTO> getDonorsIncremental(@RequestParam long since) {
        log.info("api.enter who.donors.incremental since={}", since);
        List<WhoDonorDTO> donors = repo.fetchDonorsSince(since);
        log.info("api.exit who.donors.incremental count={}", donors.size());
        return donors;
    }

    @GetMapping("/incremental")
    public WhoIncrementalResponseDTO etlIncremental(
            @RequestParam long since,
            @RequestParam long until) {
        log.info("api.enter who.incremental since={} until={}", since, until);
        WhoIncrementalResponseDTO response = new WhoIncrementalResponseDTO(
            repo.fetchEtlBanks(since, until),
            repo.fetchEtlDonors(since, until),
            repo.fetchEtlInventoryTransactions(since, until));
        log.info("api.exit who.incremental banks={} donors={} inventoryTxns={}",
            response.getBlood_banks().size(),
            response.getDonors().size(),
            response.getInventory_transactions().size());
        return response;
    }
}
