package com.hemo.backend.service;

import com.hemo.backend.entity.Request;
import com.hemo.backend.entity.Response;
import com.hemo.backend.repository.RequestRepository;
import com.hemo.backend.repository.ResponseRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DonorService {

    private static final String FIXED_REQUESTOR_PHONE = "9325834381";

    private final ResponseRepository responseRepository;
    private final RequestRepository requestRepository;

    @Value("${twilio.whatsapp-number}")
    private String twilioWhatsappNumber;

    @Transactional
    public void handleDonorReply(String donorPhone, String reply) {

        // Find the most recent pending response for this donor
        // Pending = responseStatus is NULL (notified but not yet replied)
        List<Response> pending = responseRepository.findPendingByPhone(donorPhone);

        if (pending.isEmpty()) {
            System.out.println("No pending request found for donor: " + donorPhone);
            return;
        }

        // Take the most recent one
        Response response = pending.get(0);
        Request request = response.getRequest();

        if (reply.equals("YES")) {

            // Mark response as accepted
            response.setResponseStatus("YES");
            response.setRespondedAt(LocalDateTime.now());
            responseRepository.save(response);

            // Close the request after a donor accepts it
            request.setStatus("CLOSED");
            requestRepository.save(request);

            // Send donor contact to requestor via WhatsApp
            try {
                Message.creator(
                    new PhoneNumber("whatsapp:+91" + FIXED_REQUESTOR_PHONE),
                    new PhoneNumber("whatsapp:" + twilioWhatsappNumber),
                    "HEMO-CONNECT: A donor has agreed to help!\n" +
                    "Request ID: " + request.getRequestId() + "\n" +
                    "Donor Name: " + response.getDonorName() + "\n" +
                    "Donor Contact: +91" + donorPhone + "\n" +
                    "Please contact them as soon as possible."
                ).create();

                System.out.println("Requestor notified for request: " + request.getRequestId());

            } catch (Exception e) {
                System.err.println("Failed to notify requestor: " + e.getMessage());
            }

            System.out.println("Donor " + donorPhone + " said YES for request "
                + request.getRequestId());

        } else if (reply.equals("NO")) {

            // Keep response status schema-valid for a donor decline
            response.setResponseStatus("NO");
            response.setRespondedAt(LocalDateTime.now());
            responseRepository.save(response);

            System.out.println("Donor " + donorPhone + " declined request "
                + request.getRequestId());

        } else {
            // Donor replied something other than YES/NO — ignore
            System.out.println("Unrecognised reply from " + donorPhone + ": " + reply);
        }
    }
}