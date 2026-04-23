package com.hemo.backend.controller;

import com.hemo.backend.service.DonorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donor")
@RequiredArgsConstructor
public class DonorWebhookController {

    private final DonorService donorService;

    @PostMapping(
        value = "/respond",
        consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    public ResponseEntity<Void> donorRespond(
        @RequestParam("From") String from,
        @RequestParam("Body") String body
    ) {
        String donorPhone = from
            .replace("whatsapp:", "")
            .replace("+91", "")
            .trim();

        String reply = body.trim().toUpperCase();

        donorService.handleDonorReply(donorPhone, reply);

        return ResponseEntity.ok().build();
    }
}