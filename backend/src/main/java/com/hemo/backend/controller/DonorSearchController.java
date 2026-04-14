package com.hemo.backend.controller;

import com.hemo.backend.dto.DonorSearchResponseDTO;
import com.hemo.backend.service.DonorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/donors")
@RequiredArgsConstructor
public class DonorSearchController {
    private final DonorSearchService donorSearchService;

    @GetMapping("/search")
    public DonorSearchResponseDTO search(
            @RequestParam String bloodGroup,
            @RequestParam(required = false) String pincode,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(defaultValue = "200") int limit) {
        return donorSearchService.searchCompatibleDonors(bloodGroup, pincode, offset, limit);
    }
}
