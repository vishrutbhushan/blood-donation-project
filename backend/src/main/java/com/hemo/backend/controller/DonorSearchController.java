package com.hemo.backend.controller;

import com.hemo.backend.dto.DonorSearchResponseDTO;
import com.hemo.backend.dto.DonorSearchSummaryDTO;
import com.hemo.backend.service.DonorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/donors")
@RequiredArgsConstructor
@Slf4j
public class DonorSearchController {
    private final DonorSearchService donorSearchService;

    @GetMapping("/search")
    public DonorSearchSummaryDTO search(
            @RequestParam String bloodGroup,
            @RequestParam(required = false) String pincode,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "200") int limit) {
        DonorSearchResponseDTO response = donorSearchService.searchCompatibleDonors(bloodGroup, pincode, offset, limit);
        log.info("donor search bloodGroup={} pincode={} offset={} limit={} matched={}", bloodGroup, pincode, offset, limit, response.getTotalMatched());
        return donorSearchService.toSummaryDTO(response);
    }
}
