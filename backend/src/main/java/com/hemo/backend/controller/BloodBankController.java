package com.hemo.backend.controller;

import com.hemo.backend.dto.BloodBankDTO;
import com.hemo.backend.service.BloodBankService;
import com.hemo.backend.service.PincodeGeoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping({"/blood-banks", "/api/blood-banks"})
public class BloodBankController {
    private final BloodBankService bloodBankService;
    private final PincodeGeoService pincodeGeoService;

    public BloodBankController(BloodBankService bloodBankService, PincodeGeoService pincodeGeoService) {
        this.bloodBankService = bloodBankService;
        this.pincodeGeoService = pincodeGeoService;
    }

    @GetMapping("/search")
    public List<BloodBankDTO> searchByPincode(
            @RequestParam String pincode,
            @RequestParam(required = false) String bloodGroup,
            @RequestParam(required = false) String component) {
        PincodeGeoService.GeoPoint geoPoint = pincodeGeoService.resolve(pincode)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported pincode"));

        List<BloodBankDTO> response = bloodBankService.findNearestBloodBanks(geoPoint.lat(), geoPoint.lon(), bloodGroup, component);
        return response;
    }
}
