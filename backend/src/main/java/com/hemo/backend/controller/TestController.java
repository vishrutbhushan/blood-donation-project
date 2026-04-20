package com.hemo.backend.controller;



import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TestController {

    @GetMapping("/test/send-sms")
    public String sendTestSms() {

        // 🔴 Your phone number (hardcoded)
        String to = "whatsapp:+919876543210";

        // 🔴 Twilio sandbox number
        String from = "whatsapp:+14155238886";

        Message message = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(from),
                "🚨 TEST: Can you donate blood? Reply YES or NO"
        ).create();

        return "Message sent! SID: " + message.getSid();
    }
}