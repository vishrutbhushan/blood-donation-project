package com.hemo.backend.service;

import com.hemo.backend.dto.ResponseRecordDTO;
import com.hemo.backend.dto.ResponseDTO;
import com.hemo.backend.entity.Request;
import com.hemo.backend.entity.Response;
import com.hemo.backend.exception.GlobalExceptionHandler.AppException;
import com.hemo.backend.repository.RequestRepository;
import com.hemo.backend.repository.ResponseRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
        Long safeRequestId = Objects.requireNonNull(requestId, "requestId");
        Request request = requestRepository.findById(safeRequestId)
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

        return ResponseRecordMapper.toDto(responseRepository.save(response));
    }

    @Transactional(readOnly = true)
    public List<ResponseRecordDTO> getResponsesByRequest(Long requestId) {
        return responseRepository.findByRequestId(requestId)
                .stream()
                .map(ResponseRecordMapper::toDto)
                .toList();
    }

    @Transactional
    public ResponseRecordDTO markRespondedByPhone(String phoneNumber) {
        List<Response> pending = responseRepository.findPendingByPhone(phoneNumber);
        if (pending.isEmpty()) {
            throw new AppException(HttpStatus.NOT_FOUND, "No pending donor contact found for phone number");
        }

        Response response = pending.get(0);
        response.setResponseStatus("YES");
        response.setRespondedAt(LocalDateTime.now());
        return ResponseRecordMapper.toDto(responseRepository.save(response));
    }
}
