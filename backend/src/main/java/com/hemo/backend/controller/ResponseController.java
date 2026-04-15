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
        ResponseRecordDTO response = responseService.addResponse(requestId, dto);
        log.info("response created requestId={} responseId={}", requestId, response.getResponseId());
        return response;
    }

    @GetMapping("/{requestId}")
    public List<ResponseRecordDTO> getResponses(@PathVariable Long requestId) {
        List<ResponseRecordDTO> responses = responseService.getResponsesByRequest(requestId);
        log.info("responses fetched requestId={} count={}", requestId, responses.size());
        return responses;
    }

    @PostMapping("/mark-responded")
    public ResponseRecordDTO markResponded(@Valid @RequestBody MarkResponseByPhoneDTO dto) {
        ResponseRecordDTO response = responseService.markRespondedByPhone(dto.getPhoneNumber());
        log.info("response marked responded phone={} responseId={} requestId={}", dto.getPhoneNumber(), response.getResponseId(), response.getRequestId());
        return response;
    }
}
