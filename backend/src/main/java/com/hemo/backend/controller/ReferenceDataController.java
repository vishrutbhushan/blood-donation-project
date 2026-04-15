package com.hemo.backend.controller;

import com.hemo.backend.dto.ReferenceDataDTO;
import com.hemo.backend.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reference-data")
@RequiredArgsConstructor
@Slf4j
public class ReferenceDataController {
    private final ReferenceDataService referenceDataService;

    @GetMapping
    public ReferenceDataDTO getReferenceData() {
        ReferenceDataDTO response = referenceDataService.getReferenceData();
        log.info("reference data loaded bloodGroups={} bloodComponents={}", response.getBloodGroups().size(), response.getBloodComponents().size());
        return response;
    }
}