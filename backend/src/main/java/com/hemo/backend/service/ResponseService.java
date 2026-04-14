package com.hemo.backend.service;

import com.hemo.backend.dto.ResponseRecordDTO;
import com.hemo.backend.dto.ResponseDTO;
import com.hemo.backend.entity.Request;
import com.hemo.backend.entity.Response;
import com.hemo.backend.exception.GlobalExceptionHandler.AppException;
import com.hemo.backend.repository.RequestRepository;
import com.hemo.backend.repository.ResponseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResponseService {

    private final ResponseRepository responseRepository;
    private final RequestRepository requestRepository;

    @Transactional
    public ResponseRecordDTO addResponse(Long requestId, ResponseDTO dto) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Request not found"));

        Response response = new Response();
        response.setRequest(request);
        response.setDonorId(dto.getDonorId());
        response.setDonorName(dto.getDonorName());
        response.setAbhaId(dto.getAbhaId());
        response.setPhoneNumber(dto.getPhoneNumber());
        response.setBloodGroup(dto.getBloodGroup());
        response.setLocation(dto.getLocation());
        response.setResponseStatus(dto.getResponseStatus());

        return toRecord(responseRepository.save(response));
    }

    @Transactional(readOnly = true)
    public List<ResponseRecordDTO> getResponsesByRequest(Long requestId) {
        return responseRepository.findByRequestId(requestId)
                .stream()
                .map(this::toRecord)
                .toList();
    }

    private ResponseRecordDTO toRecord(Response response) {
        return ResponseRecordDTO.builder()
                .responseId(response.getResponseId())
                .requestId(response.getRequest().getRequestId())
                .donorId(response.getDonorId())
                .donorName(response.getDonorName())
                .abhaId(response.getAbhaId())
                .phoneNumber(response.getPhoneNumber())
                .bloodGroup(response.getBloodGroup())
                .location(response.getLocation())
                .responseStatus(response.getResponseStatus())
                .respondedAt(response.getRespondedAt())
                .build();
    }
}
