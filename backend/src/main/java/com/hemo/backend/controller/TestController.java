package com.hemo.backend.controller;

import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

@RestController
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @Value("${app.test-whatsapp-to-phone}")
    private String testWhatsappToPhone;

    @Value("${twilio.whatsapp-number}")
    private String twilioWhatsappNumber;

    @GetMapping("/test/send-sms")
    public String sendTestSms() {
        log.info("api.enter test.send-sms");

        String to = "whatsapp:+91" + testWhatsappToPhone;
        String from = "whatsapp:" + twilioWhatsappNumber;

        Message message = Message.creator(
                new PhoneNumber(to),
                new PhoneNumber(from),
                "🚨 TEST: Can you donate blood? Reply YES or NO"
        ).create();

        log.info("api.exit test.send-sms sid={}", message.getSid());
        return "Message sent! SID: " + message.getSid();
    }
}