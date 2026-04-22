package com.hemo.backend.controller;

import com.hemo.backend.dto.AuthProfileDTO;
import com.hemo.backend.dto.OtpSendRequestDTO;
import com.hemo.backend.dto.OtpSendResponseDTO;
import com.hemo.backend.dto.OtpVerifyRequestDTO;
import com.hemo.backend.dto.OtpVerifyResponseDTO;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
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

    private static final int OTP_TTL_MINUTES = 5;

    private final Map<String, OtpRecord> otpStore = new ConcurrentHashMap<>();

    @Value("${app.fixed-otp-receiver-phone}")
    private String fixedOtpReceiverPhone;

    @Value("${twilio.whatsapp-number}")
    private String twilioWhatsappNumber;

    @PostMapping("/send-otp")
    public OtpSendResponseDTO sendOtp(@Valid @RequestBody OtpSendRequestDTO dto) {
        log.info("api.enter auth.send-otp abhaId={}", dto.getAbhaId());
        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES);
        otpStore.put(dto.getAbhaId(), new OtpRecord(otp, expiresAt));

        sendOtpToFixedWhatsapp(otp);

        AuthProfileDTO profile = profileFromAbha(dto.getAbhaId());
        OtpSendResponseDTO response = new OtpSendResponseDTO(true, dto.getAbhaId(), profile.getName(), profile.getPhone());
        log.info("api.exit auth.send-otp sent={}", response.isSent());
        return response;
    }

    @PostMapping("/verify-otp")
    public OtpVerifyResponseDTO verifyOtp(@Valid @RequestBody OtpVerifyRequestDTO dto) {
        log.info("api.enter auth.verify-otp abhaId={}", dto.getAbhaId());
        OtpRecord record = otpStore.get(dto.getAbhaId());
        if (record == null || record.expiresAt().isBefore(LocalDateTime.now())) {
            otpStore.remove(dto.getAbhaId());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP expired or not found");
        }
        if (!record.otp().equals(dto.getOtp())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid OTP");
        }

        // One-time OTP consumption on successful verification.
        otpStore.remove(dto.getAbhaId());

        AuthProfileDTO profile = profileFromAbha(dto.getAbhaId());
        OtpVerifyResponseDTO response = new OtpVerifyResponseDTO(true, dto.getAbhaId(), profile.getName(), profile.getPhone());
        log.info("api.exit auth.verify-otp verified={}", response.isVerified());
        return response;
    }

    private String generateOtp() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));
    }

    private void sendOtpToFixedWhatsapp(String otp) {
        String receiver = "whatsapp:+91" + fixedOtpReceiverPhone;
        String sender = "whatsapp:" + twilioWhatsappNumber;
        try {
            Message.creator(
                new PhoneNumber(receiver),
                new PhoneNumber(sender),
                "HEMO-CONNECT OTP: " + otp + "\nValid for " + OTP_TTL_MINUTES + " minutes."
            ).create();
        } catch (Exception ex) {
            log.error("auth.send-otp.whatsapp-failed receiver={} reason={}", fixedOtpReceiverPhone, ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OTP delivery failed");
        }
    }

    private AuthProfileDTO profileFromAbha(String abhaId) {
        String trimmedPhone = abhaId.substring(0, 10);
        return new AuthProfileDTO("Demo User", trimmedPhone);
    }

    private record OtpRecord(String otp, LocalDateTime expiresAt) {}
}
