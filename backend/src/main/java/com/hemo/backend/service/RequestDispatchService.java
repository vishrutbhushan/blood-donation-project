package com.hemo.backend.service;

import com.hemo.backend.dto.DonorCandidateDTO;
import com.hemo.backend.dto.DonorSearchResponseDTO;
import com.hemo.backend.entity.Request;
import com.hemo.backend.entity.Response;
import com.hemo.backend.repository.ResponseRepository;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestDispatchService {
    private static final int INITIAL_BATCH_SIZE = 20;

    private final ResponseRepository responseRepository;
    private final DonorSearchService donorSearchService;

    @Value("${app.demo-donor-phone}")
    private String demoDonorPhone;

    @Value("${app.demo-donor-name}")
    private String demoDonorName;

    @Value("${twilio.whatsapp-number}")
    private String twilioWhatsappNumber;

    private String demoDonorId() {
        return "DEMO:" + demoDonorPhone;
    }

    public int dispatchBatchForRequest(Request request, int batchSize) {
        if (batchSize <= 0) {
            return 0;
        }

        List<String> excludedDonorIds = responseRepository.findContactedDonorIdsBySearchId(request.getSearch().getSearchId());
        String demoId = demoDonorId();
        DonorSearchResponseDTO donorBatch = donorSearchService.searchCompatibleDonors(
                request.getBloodGroup(),
                request.getSearch().getHospitalPincode(),
                0,
                INITIAL_BATCH_SIZE,
                excludedDonorIds
        );

        List<DonorCandidateDTO> dispatchDonors = donorBatch.getDonors() == null
            ? new ArrayList<>()
            : new ArrayList<>(donorBatch.getDonors());

        boolean demoAlreadyContacted = excludedDonorIds.contains(demoId);
        if (!demoAlreadyContacted) {
            dispatchDonors.add(0, DonorCandidateDTO.builder()
                .donorId(demoId)
                .name(demoDonorName)
                .bloodGroup(request.getBloodGroup())
                .phone(demoDonorPhone)
                .pincode(request.getSearch().getHospitalPincode())
                .location("Demo")
                .source("DEMO")
                .distanceKm(0.0)
                .build());
        }

        int sent = 0;
        for (DonorCandidateDTO donor : dispatchDonors) {
            if (sent >= batchSize) {
                break;
            }

            Response contact_number = new Response();
            contact_number.setRequest(request);
            contact_number.setDonorId(donor.getDonorId());
            contact_number.setAbhaId(donor.getAbhaId());
            contact_number.setDonorName(donor.getName());
            contact_number.setPhoneNumber(donor.getPhone());
            contact_number.setBloodGroup(donor.getBloodGroup());
            contact_number.setLocation(donor.getLocation());
            responseRepository.save(contact_number);

            if (demoId.equals(donor.getDonorId())) {
                try {
                    sendWhatsAppToDonor(
                        demoDonorPhone,
                        request.getBloodGroup(),
                        request.getSearch().getHospitalName(),
                        request.getSearch().getHospitalPincode()
                    );
                } catch (Exception e) {
                    System.err.println("WhatsApp failed for demo donor "
                        + donor.getDonorId() + ": " + e.getMessage());
                }
            } else {
                System.out.println("Skipped WhatsApp send for donor " + donor.getDonorId()
                    + " phone=" + donor.getPhone());
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