package com.hemo.backend.controller;

import com.hemo.backend.dto.ResponseRecordDTO;
import com.hemo.backend.dto.ResponseDTO;
import com.hemo.backend.dto.MarkResponseByPhoneDTO;
import com.hemo.backend.service.ResponseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/responses")
@RequiredArgsConstructor
@Slf4j
public class ResponseController {

    private final ResponseService responseService;

    @PostMapping("/{requestId}")
    public ResponseRecordDTO addResponse(@PathVariable Long requestId,
                               @Valid @RequestBody ResponseDTO dto) {
        log.info("api.enter responses.create requestId={}", requestId);
        ResponseRecordDTO response = responseService.addResponse(requestId, dto);
        log.info("api.exit responses.create responseId={}", response.getResponseId());
        return response;
    }

    @GetMapping("/{requestId}")
    public List<ResponseRecordDTO> getResponses(@PathVariable Long requestId) {
        log.info("api.enter responses.list requestId={}", requestId);
        List<ResponseRecordDTO> responses = responseService.getResponsesByRequest(requestId);
        log.info("api.exit responses.list count={}", responses.size());
        return responses;
    }

    @PostMapping("/mark-responded")
    public ResponseRecordDTO markResponded(@Valid @RequestBody MarkResponseByPhoneDTO dto) {
        log.info("api.enter responses.mark-responded phone={}", dto.getPhoneNumber());
        ResponseRecordDTO response = responseService.markRespondedByPhone(dto.getPhoneNumber());
        log.info("api.exit responses.mark-responded responseId={} requestId={}", response.getResponseId(), response.getRequestId());
        return response;
    }
}
