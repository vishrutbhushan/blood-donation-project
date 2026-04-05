package com.hemo.backend.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.hemo.backend.dto.ResponseDTO;
import com.hemo.backend.entity.Response;
import com.hemo.backend.entity.Request;
import com.hemo.backend.exception.ResourceNotFoundException;
import com.hemo.backend.repository.RequestRepository;
import com.hemo.backend.repository.ResponseRepository;
import java.util.List;



@Service
@RequiredArgsConstructor
public class ResponseService {

    private final ResponseRepository responseRepository;
    private final RequestRepository requestRepository;

    public Response addResponse(Long requestId, ResponseDTO dto) {

        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found"));

        Response response = new Response();
        response.setRequest(request);
        response.setDonorId(dto.getDonorId());
        response.setDonorName(dto.getDonorName());
        response.setPhoneNumber(dto.getPhoneNumber());
        response.setBloodGroup(dto.getBloodGroup());
        response.setLocation(dto.getLocation());
        response.setResponseStatus(dto.getResponseStatus());

        return responseRepository.save(response);
    }

    public List<Response> getResponsesByRequest(Long requestId) {
    return responseRepository.findByRequestRequestId(requestId);
}

}