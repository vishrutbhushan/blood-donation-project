package com.hemo.backend.controller;

import com.hemo.backend.dto.ActiveRequestStatusDTO;
import com.hemo.backend.dto.DispatchResultDTO;
import com.hemo.backend.dto.DonorSearchSummaryDTO;
import com.hemo.backend.dto.RequestDTO;
import com.hemo.backend.dto.RequestSummaryDTO;
import com.hemo.backend.dto.ResponseRecordDTO;
import com.hemo.backend.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
@Slf4j
public class RequestController {

    private final RequestService requestService;

    @PostMapping("/{searchId}")
    public RequestSummaryDTO createRequest(@PathVariable Long searchId,
                                @Valid @RequestBody RequestDTO dto) {
        RequestSummaryDTO response = requestService.createRequest(searchId, dto);
        log.info("request created searchId={} requestId={}", searchId, response.getRequestId());
        return response;
    }

    @PostMapping("/{requestId}/re-request")
    public RequestSummaryDTO reRequest(@PathVariable Long requestId) {
        RequestSummaryDTO response = requestService.reRequest(requestId);
        log.info("request re-requested requestId={} newRequestId={}", requestId, response.getRequestId());
        return response;
    }

    @GetMapping("/{requestId}/re-request-preview")
    public DonorSearchSummaryDTO reRequestPreview(@PathVariable Long requestId) {
        DonorSearchSummaryDTO preview = requestService.previewReRequest(requestId);
        log.info("request re-request preview requestId={} matched={}", requestId, preview.getTotalMatched());
        return preview;
    }

    @PostMapping("/{requestId}/dispatch-next")
    public DispatchResultDTO dispatchNext(@PathVariable Long requestId) {
        DispatchResultDTO response = requestService.dispatchNextTwenty(requestId);
        log.info("request dispatch-next requestId={} notifiedFrom={} notifiedTo={}", requestId, response.getNotifiedFrom(), response.getNotifiedTo());
        return response;
    }

    @GetMapping("/user/{userId}")
    public List<RequestSummaryDTO> getUserRequests(@PathVariable Long userId) {
        List<RequestSummaryDTO> requests = requestService.getUserRequestHistory(userId);
        log.info("request history fetched userId={} count={}", userId, requests.size());
        return requests;
    }

    @GetMapping("/user/{userId}/responses")
    public List<ResponseRecordDTO> getUserResponses(@PathVariable Long userId) {
        List<ResponseRecordDTO> responses = requestService.getUserResponses(userId);
        log.info("request responses fetched userId={} count={}", userId, responses.size());
        return responses;
    }

    @GetMapping("/user/{userId}/active")
    public ActiveRequestStatusDTO hasActive(@PathVariable Long userId) {
        boolean active = requestService.hasActiveRequestForUser(userId);
        log.info("request active-status userId={} active={}", userId, active);
        return new ActiveRequestStatusDTO(active);
    }
}
