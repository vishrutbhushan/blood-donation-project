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

    private final ResponseRepository responseRepository;
    private final RequestRepository requestRepository;

    @Value("${app.fixed-requestor-phone}")
    private String fixedRequestorPhone;

    @Value("${twilio.whatsapp-number}")
    private String twilioWhatsappNumber;

    @Transactional
    public void handleDonorReply(String donorPhone, String reply) {

        List<Response> pending = responseRepository.findPendingByPhone(donorPhone);

        if (pending.isEmpty()) {
            System.out.println("No pending request found for donor: " + donorPhone);
            return;
        }

        Response response = pending.get(0);
        Request request = response.getRequest();

        if (reply.equals("YES")) {

            response.setResponseStatus("YES");
            response.setRespondedAt(LocalDateTime.now());
            responseRepository.save(response);

            request.setStatus("CLOSED");
            requestRepository.save(request);

            try {
                Message.creator(
                    new PhoneNumber("whatsapp:+91" + fixedRequestorPhone),
                    new PhoneNumber("whatsapp:" + twilioWhatsappNumber),
                    "HEMO-CONNECT: A donor has agreed to help!\n" +
                    "Request ID: " + request.getRequestId() + "\n" +
                    "Donor Name: " + response.getDonorName() + "\n" +
                    "Donor Contact: +91" + donorPhone + "."
                ).create();

                System.out.println("Requestor notified for request: " + request.getRequestId());

            } catch (Exception e) {
                System.err.println("Failed to notify requestor " + fixedRequestorPhone + ": " + e.getMessage());
            }

            System.out.println("Donor " + donorPhone + " said YES for request "
                + request.getRequestId());

        } else if (reply.equals("NO")) {

            response.setResponseStatus("NO");
            response.setRespondedAt(LocalDateTime.now());
            responseRepository.save(response);

            System.out.println("Donor " + donorPhone + " declined request "
                + request.getRequestId());

        } else {
            System.out.println("Unrecognised reply from " + donorPhone + ": " + reply);
        }
    }
}