package com.hemo.backend.controller;

import com.hemo.backend.dto.BloodBankDTO;
import com.hemo.backend.service.BloodBankService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blood-banks")
public class BloodBankController {
    private final BloodBankService bloodBankService;

    public BloodBankController(BloodBankService bloodBankService) {
        this.bloodBankService = bloodBankService;
    }

    @GetMapping("/nearest-20")
    public List<BloodBankDTO> getNearestBloodBanks(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        return bloodBankService.findNearestBloodBanks(latitude, longitude);
    }
}
