package com.hemo.backend.controller;

import com.hemo.backend.service.DonorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donor")
@RequiredArgsConstructor
@Slf4j
public class DonorWebhookController {

    private final DonorService donorService;

    // Twilio sends form-encoded POST, not JSON
    @PostMapping(
        value = "/respond",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<Void> donorRespond(
        @RequestParam("From") String from,
        @RequestParam("Body") String body
    ) {
        // Strip "whatsapp:+91" prefix that Twilio adds
        // From arrives as: whatsapp:+919876543210
        String donorPhone = from
            .replace("whatsapp:", "")
            .replace("+91", "")
            .trim();

        String reply = body.trim().toUpperCase();

        log.info("api.enter donor.respond phone={} reply={}", donorPhone, reply);

        donorService.handleDonorReply(donorPhone, reply);

        log.info("api.exit donor.respond phone={}", donorPhone);

        // Return an empty response so Twilio does not send any message back.
        return ResponseEntity.ok().build();
    }
}