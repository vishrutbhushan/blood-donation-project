package com.hemo.backend.controller;

import com.hemo.backend.dto.AuthProfileDTO;
import com.hemo.backend.dto.OtpSendRequestDTO;
import com.hemo.backend.dto.OtpSendResponseDTO;
import com.hemo.backend.dto.OtpVerifyRequestDTO;
import com.hemo.backend.dto.OtpVerifyResponseDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private static final String HARDCODED_OTP = "123456";

    @PostMapping("/send-otp")
    public OtpSendResponseDTO sendOtp(@Valid @RequestBody OtpSendRequestDTO dto) {
        log.info("api.enter auth.send-otp abhaId={}", dto.getAbhaId());
        AuthProfileDTO profile = profileFromAbha(dto.getAbhaId());
        OtpSendResponseDTO response = new OtpSendResponseDTO(true, dto.getAbhaId(), profile.getName(), profile.getPhone());
        log.info("api.exit auth.send-otp sent={}", response.isSent());
        return response;
    }

    @PostMapping("/verify-otp")
    public OtpVerifyResponseDTO verifyOtp(@Valid @RequestBody OtpVerifyRequestDTO dto) {
        log.info("api.enter auth.verify-otp abhaId={}", dto.getAbhaId());
        if (!HARDCODED_OTP.equals(dto.getOtp())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
        }

        AuthProfileDTO profile = profileFromAbha(dto.getAbhaId());
        OtpVerifyResponseDTO response = new OtpVerifyResponseDTO(true, dto.getAbhaId(), profile.getName(), profile.getPhone());
        log.info("api.exit auth.verify-otp verified={}", response.isVerified());
        return response;
    }

    private AuthProfileDTO profileFromAbha(String abhaId) {
        String trimmedPhone = abhaId.substring(0, 10);
        return new AuthProfileDTO("Demo User", trimmedPhone);
    }
}
