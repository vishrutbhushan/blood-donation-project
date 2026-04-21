package com.who.backend.controller;

import com.who.backend.dto.WhoIncrementalResponseDTO;
import com.who.backend.repository.WhoRepository;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhoController {

    private static final Logger log = LoggerFactory.getLogger(WhoController.class);

    private final WhoRepository repo;

    public WhoController(WhoRepository repo) {
        this.repo = repo;
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

    @GetMapping("/incremental/day")
    public WhoIncrementalResponseDTO etlIncrementalByDay(@RequestParam String date) {
        log.info("api.enter who.incremental.day date={}", date);
        WhoIncrementalResponseDTO response = new WhoIncrementalResponseDTO(
            repo.fetchEtlBanksByDate(date),
            repo.fetchEtlDonorsByDate(date),
            repo.fetchEtlInventoryTransactionsByDate(date));
        log.info("api.exit who.incremental.day banks={} donors={} inventoryTxns={}",
            response.getBlood_banks().size(),
            response.getDonors().size(),
            response.getInventory_transactions().size());
        return response;
    }

    @GetMapping("/incremental/month")
    public WhoIncrementalResponseDTO etlIncrementalByMonth(@RequestParam String month) {
        YearMonth parsedMonth = YearMonth.parse(month);
        log.info("api.enter who.incremental.month month={}", parsedMonth);
        WhoIncrementalResponseDTO response = new WhoIncrementalResponseDTO(
            repo.fetchEtlBanksByMonth(parsedMonth.toString()),
            repo.fetchEtlDonorsByMonth(parsedMonth.toString()),
            repo.fetchEtlInventoryTransactionsByMonth(parsedMonth.toString()));
        log.info("api.exit who.incremental.month banks={} donors={} inventoryTxns={}",
            response.getBlood_banks().size(),
            response.getDonors().size(),
            response.getInventory_transactions().size());
        return response;
    }
}
