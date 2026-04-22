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

    @Value("${app.demo-donor-phone-1}")
    private String donorPhoneOne;

    @Value("${app.demo-donor-phone-2}")
    private String donorPhoneTwo;

    @Value("${app.demo-donor-phone-3}")
    private String donorPhoneThree;

    @Value("${twilio.whatsapp-number}")
    private String twilioWhatsappNumber;

    private List<String> sandboxDonorPhones() {
        return List.of(donorPhoneOne, donorPhoneTwo, donorPhoneThree);
    }

    public int dispatchBatchForRequest(Request request, int batchSize) {
        if (batchSize <= 0) {
            return 0;
        }

        List<String> excludedDonorIds = responseRepository.findContactedDonorIdsBySearchId(request.getSearch().getSearchId());
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

        int sent = 0;
        List<String> donorPhones = sandboxDonorPhones();
        for (int index = 0; index < dispatchDonors.size(); index++) {
            DonorCandidateDTO donor = dispatchDonors.get(index);
            if (sent >= batchSize) {
                break;
            }

            String resolvedPhone = index < donorPhones.size() ? donorPhones.get(index) : donor.getPhone();

            Response contact_number = new Response();
            contact_number.setRequest(request);
            contact_number.setDonorId(donor.getDonorId());
            contact_number.setAbhaId(normalizeAbhaId(donor));
            contact_number.setDonorName(donor.getName());
            contact_number.setPhoneNumber(resolvedPhone);
            contact_number.setBloodGroup(donor.getBloodGroup());
            contact_number.setLocation(donor.getLocation());
            responseRepository.save(contact_number);

            if (index < donorPhones.size()) {
                try {
                    sendWhatsAppToDonor(
                        resolvedPhone,
                        request.getBloodGroup(),
                        request.getSearch().getHospitalName(),
                        request.getSearch().getHospitalPincode()
                    );
                } catch (Exception e) {
                    System.err.println("WhatsApp failed for sandbox donor "
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
            "Urgent blood " + bloodGroup + " required at hospital " + safeHospitalName + " " + safePincode + ". " +
            "Reply YES to share contact details"
        ).create();

        System.out.println("WhatsApp sent to +91" + donorPhone);
    }

    private String normalizeAbhaId(DonorCandidateDTO donor) {
        String abhaId = donor.getAbhaId();
        if (abhaId != null) {
            String trimmed = abhaId.trim();
            if (!trimmed.isBlank()) {
                return trimmed;
            }
        }

        String donorId = donor.getDonorId() == null ? "" : donor.getDonorId().trim();
        String phone = donor.getPhone() == null ? "" : donor.getPhone().trim();
        long value = Math.floorMod((donorId + "|" + phone).hashCode(), 100000000000000L);
        return String.format("%014d", value);
    }
}