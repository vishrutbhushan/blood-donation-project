package com.hemo.backend.controller;

import com.hemo.backend.dto.ReferenceDataDTO;
import com.hemo.backend.service.ReferenceDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reference-data")
@RequiredArgsConstructor
public class ReferenceDataController {
    private final ReferenceDataService referenceDataService;

    @GetMapping
    public ReferenceDataDTO getReferenceData() {
        return referenceDataService.getReferenceData();
    }
}