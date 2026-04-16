package com.hemo.backend.service;

import com.hemo.backend.dto.ResponseRecordDTO;
import com.hemo.backend.entity.Response;

public final class ResponseRecordMapper {
    private ResponseRecordMapper() {}

    public static ResponseRecordDTO toDto(Response response) {
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
