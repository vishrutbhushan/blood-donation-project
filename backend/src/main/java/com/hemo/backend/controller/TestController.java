package com.hemo.backend.controller;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @GetMapping("/test/send-sms")
    public String sendTestSms() {
        log.info("api.enter test.send-sms");

        // 🔴 Your phone number (hardcoded)
        String to = "whatsapp:+919325834381";

        // 🔴 Twilio sandbox number
        String from = "whatsapp:+14155238886";

        Message message = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(from),
                "🚨 TEST: Can you donate blood? Reply YES or NO"
        ).create();

        log.info("api.exit test.send-sms sid={}", message.getSid());
        return "Message sent! SID: " + message.getSid();
    }
}