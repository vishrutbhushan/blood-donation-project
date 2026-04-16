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
@RequestMapping({"/blood-banks", "/api/blood-banks"})
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
        log.info("api.enter blood-banks.nearest-20 latitude={} longitude={}", latitude, longitude);
        List<BloodBankDTO> banks = bloodBankService.findNearestBloodBanks(latitude, longitude);
        log.info("api.exit blood-banks.nearest-20 count={}", banks.size());
        return banks;
    }

    @GetMapping("/search")
    public List<BloodBankDTO> searchByPincode(
            @RequestParam String pincode,
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String component) {
        log.info("api.enter blood-banks.search pincode={} bloodGroup={} component={}", pincode, bloodGroup, component);
        PincodeGeoService.GeoPoint geoPoint = pincodeGeoService.resolve(pincode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported pincode"));

        List<BloodBankDTO> nearest = bloodBankService.findNearestBloodBanks(geoPoint.lat(), geoPoint.lon(), bloodGroup, component);
        List<BloodBankDTO> exactPincode = nearest.stream()
                .filter(bank -> pincode.equals(bank.getPincode()))
                .toList();

        List<BloodBankDTO> response = exactPincode.isEmpty() ? nearest : exactPincode;
        log.info("api.exit blood-banks.search count={}", response.size());
        return response;
    }
}
