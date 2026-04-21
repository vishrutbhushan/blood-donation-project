package com.hemo.backend.service;

import com.hemo.backend.dto.DonorCandidateDTO;
import com.hemo.backend.dto.DonorSearchResponseDTO;
import com.hemo.backend.entity.Request;
import com.hemo.backend.entity.Response;
import com.hemo.backend.repository.ResponseRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestDispatchService {
    private static final String DEMO_DONOR_ID = "DEMO:9325834381";
    private static final String DEMO_DONOR_PHONE = "9325834381";

    private final ResponseRepository responseRepository;
    private final DonorSearchService donorSearchService;

    @Value("${twilio.whatsapp-number}")
    private String twilioWhatsappNumber;

    public int dispatchBatchForRequest(Request request, int batchSize) {
        if (batchSize <= 0) {
            return 0;
        }

        List<String> excludedDonorIds = responseRepository.findContactedDonorIdsBySearchId(request.getSearch().getSearchId());
        DonorSearchResponseDTO donorBatch = donorSearchService.searchCompatibleDonors(
                request.getBloodGroup(),
                request.getSearch().getHospitalPincode(),
                0,
                200,
                excludedDonorIds
        );

        int realDonorCount = donorBatch.getDonors() == null ? 0 : donorBatch.getDonors().size();
        boolean demoAlreadyContacted = excludedDonorIds.contains(DEMO_DONOR_ID);
        if (realDonorCount > 0 && !demoAlreadyContacted && donorBatch.getDonors() != null) {
            donorBatch.getDonors().add(0, DonorCandidateDTO.builder()
            .donorId(DEMO_DONOR_ID)
            .name("Demo Donor")
            .bloodGroup(request.getBloodGroup())
            .phone(DEMO_DONOR_PHONE)
            .pincode(request.getSearch().getHospitalPincode())
            .location("Demo")
            .source("DEMO")
            .distanceKm(0.0)
            .build());
        }

        int sent = 0;
        for (DonorCandidateDTO donor : donorBatch.getDonors()) {
            if (sent >= batchSize) {
                break;
            }

            Response contact_number = new Response();
            contact_number.setRequest(request);
            contact_number.setDonorId(donor.getDonorId());
            contact_number.setDonorName(donor.getName());
            contact_number.setPhoneNumber(donor.getPhone());
            contact_number.setBloodGroup(donor.getBloodGroup());
            contact_number.setLocation(donor.getLocation());
            contact_number.setResponseStatus("NO");
            responseRepository.save(contact_number);

            // Send WhatsApp notification to donor
            if (donor.getPhone() != null && !donor.getPhone().isBlank()) {
                try {
                    sendWhatsAppToDonor(
                        donor.getPhone(),
                        request.getBloodGroup(),
                        request.getSearch().getHospitalName(),
                        request.getSearch().getHospitalPincode()
                    );
                } catch (Exception e) {
                    System.err.println("WhatsApp failed for donor "
                        + donor.getDonorId() + ": " + e.getMessage());
                }
            } else {
                System.err.println("No phone number for donor: " + donor.getDonorId());
            }

            sent++;
        }

        return sent;
    }

    private void sendWhatsAppToDonor(String donorPhone, String bloodGroup,
                                      String hospitalName, String pincode) {
        String safeHospitalName = hospitalName == null || hospitalName.isBlank() ? "Hospital" : hospitalName.trim();
        String safePincode = pincode == null ? "" : pincode.trim();
        Message.creator(
            new PhoneNumber("whatsapp:+91" + donorPhone),
            new PhoneNumber("whatsapp:" + twilioWhatsappNumber),
            "Urgent " + bloodGroup + " required at " + safeHospitalName + " " + safePincode + ". " +
            "Reply YES to share contact details"
        ).create();

        System.out.println("WhatsApp sent to +91" + donorPhone);
    }
}