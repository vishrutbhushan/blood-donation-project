package com.hemo.backend.controller;

import com.hemo.backend.dto.BloodBankDTO;
import com.hemo.backend.service.BloodBankService;
import com.hemo.backend.service.PincodeGeoService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/blood-banks")
@Slf4j
public class BloodBankController {
    private final BloodBankService bloodBankService;
    private final PincodeGeoService pincodeGeoService;

    public BloodBankController(BloodBankService bloodBankService, PincodeGeoService pincodeGeoService) {
        this.bloodBankService = bloodBankService;
        this.pincodeGeoService = pincodeGeoService;
    }

    @GetMapping("/nearest-20")
    public List<BloodBankDTO> getNearestBloodBanks(
            @RequestParam Double latitude,
            @RequestParam Double longitude) {
        List<BloodBankDTO> banks = bloodBankService.findNearestBloodBanks(latitude, longitude);
        log.info("nearest blood banks requested latitude={} longitude={} returned={}", latitude, longitude, banks.size());
        return banks;
    }

    @GetMapping("/search")
    public List<BloodBankDTO> searchByPincode(@RequestParam String pincode) {
        PincodeGeoService.GeoPoint geoPoint = pincodeGeoService.resolve(pincode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported pincode"));

        List<BloodBankDTO> nearest = bloodBankService.findNearestBloodBanks(geoPoint.lat(), geoPoint.lon());
        List<BloodBankDTO> exactPincode = nearest.stream()
                .filter(bank -> pincode.equals(bank.getPincode()))
                .toList();

        List<BloodBankDTO> response = exactPincode.isEmpty() ? nearest : exactPincode;
        log.info("blood bank search by pincode={} returned={}", pincode, response.size());
        return response;
    }
}
