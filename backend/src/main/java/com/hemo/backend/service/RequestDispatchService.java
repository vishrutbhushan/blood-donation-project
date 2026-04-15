package com.hemo.backend.service;

import com.hemo.backend.dto.DonorCandidateDTO;
import com.hemo.backend.dto.DonorSearchResponseDTO;
import com.hemo.backend.entity.Request;
import com.hemo.backend.entity.Response;
import com.hemo.backend.repository.ResponseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RequestDispatchService {
    private final ResponseRepository responseRepository;
    private final DonorSearchService donorSearchService;

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

        int sent = 0;
        for (DonorCandidateDTO donor : donorBatch.getDonors()) {
            if (sent >= batchSize) {
                break;
            }

            Response contact = new Response();
            contact.setRequest(request);
            contact.setDonorId(donor.getDonorId());
            contact.setDonorName(donor.getName());
            contact.setPhoneNumber(donor.getPhone());
            contact.setBloodGroup(donor.getBloodGroup());
            contact.setLocation(donor.getLocation());
            contact.setResponseStatus("NO");
            responseRepository.save(contact);
            sent++;
        }

        return sent;
    }
}