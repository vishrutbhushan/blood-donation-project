package com.hemo.backend.controller;

import com.hemo.backend.dto.OtpSendRequestDTO;
import com.hemo.backend.dto.OtpVerifyRequestDTO;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final String HARDCODED_OTP = "1234";

    @PostMapping("/send-otp")
    public Map<String, Object> sendOtp(@Valid @RequestBody OtpSendRequestDTO dto) {
        return Map.of(
                "sent", true,
                "message", "OTP sent",
                "phone", dto.getPhone());
    }

    @PostMapping("/verify-otp")
    public Map<String, Object> verifyOtp(@Valid @RequestBody OtpVerifyRequestDTO dto) {
        if (!HARDCODED_OTP.equals(dto.getOtp())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
        }

        return Map.of(
                "verified", true,
                "message", "OTP verified",
                "phone", dto.getPhone());
    }
}
