package com.redcross.backend.controller;

import com.redcross.backend.dto.RedcrossIncrementalResponseDTO;
import com.redcross.backend.repository.RedcrossRepository;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedcrossController {

    private static final Logger log = LoggerFactory.getLogger(RedcrossController.class);

    private final RedcrossRepository repo;

    public RedcrossController(RedcrossRepository repo) {
        this.repo = repo;
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

    @GetMapping("/incremental/day")
    public RedcrossIncrementalResponseDTO etlIncrementalByDay(@RequestParam String date) {
        log.info("api.enter redcross.incremental.day date={}", date);
        RedcrossIncrementalResponseDTO response = new RedcrossIncrementalResponseDTO(
            repo.fetchEtlBanksByDate(date),
            repo.fetchEtlDonorsByDate(date),
            repo.fetchEtlInventoryTransactionsByDate(date));
        log.info("api.exit redcross.incremental.day centres={} people={} inventoryTxns={}",
            response.getCentres().size(),
            response.getPeople().size(),
            response.getInventory_transactions().size());
        return response;
    }

    @GetMapping("/incremental/month")
    public RedcrossIncrementalResponseDTO etlIncrementalByMonth(@RequestParam String month) {
        YearMonth parsedMonth = YearMonth.parse(month);
        log.info("api.enter redcross.incremental.month month={}", parsedMonth);
        RedcrossIncrementalResponseDTO response = new RedcrossIncrementalResponseDTO(
            repo.fetchEtlBanksByMonth(parsedMonth.toString()),
            repo.fetchEtlDonorsByMonth(parsedMonth.toString()),
            repo.fetchEtlInventoryTransactionsByMonth(parsedMonth.toString()));
        log.info("api.exit redcross.incremental.month centres={} people={} inventoryTxns={}",
            response.getCentres().size(),
            response.getPeople().size(),
            response.getInventory_transactions().size());
        return response;
    }
}
