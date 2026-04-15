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
        AuthProfileDTO profile = profileFromAbha(dto.getAbhaId());
        log.info("auth send-otp requested");
        return new OtpSendResponseDTO(true, dto.getAbhaId(), profile.getName(), profile.getPhone());
    }

    @PostMapping("/verify-otp")
    public OtpVerifyResponseDTO verifyOtp(@Valid @RequestBody OtpVerifyRequestDTO dto) {
        if (!HARDCODED_OTP.equals(dto.getOtp())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
        }

        AuthProfileDTO profile = profileFromAbha(dto.getAbhaId());
        log.info("auth verify-otp succeeded");
        return new OtpVerifyResponseDTO(true, dto.getAbhaId(), profile.getName(), profile.getPhone());
    }

    private AuthProfileDTO profileFromAbha(String abhaId) {
        String trimmedPhone = abhaId.substring(0, 10);
        return new AuthProfileDTO("Demo User", trimmedPhone);
    }
}
