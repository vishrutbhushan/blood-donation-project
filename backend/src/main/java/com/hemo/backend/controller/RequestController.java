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
        log.info("api.enter requests.create searchId={}", searchId);
        RequestSummaryDTO response = requestService.createRequest(searchId, dto);
        log.info("api.exit requests.create requestId={}", response.getRequestId());
        return response;
    }

    @PostMapping("/{requestId}/re-request")
    public RequestSummaryDTO reRequest(@PathVariable Long requestId) {
        log.info("api.enter requests.re-request requestId={}", requestId);
        RequestSummaryDTO response = requestService.reRequest(requestId);
        log.info("api.exit requests.re-request newRequestId={}", response.getRequestId());
        return response;
    }

    @GetMapping("/{requestId}/re-request-preview")
    public DonorSearchSummaryDTO reRequestPreview(@PathVariable Long requestId) {
        log.info("api.enter requests.re-request-preview requestId={}", requestId);
        DonorSearchSummaryDTO preview = requestService.previewReRequest(requestId);
        log.info("api.exit requests.re-request-preview matched={}", preview.getTotalMatched());
        return preview;
    }

    @PostMapping("/{requestId}/dispatch-next")
    public DispatchResultDTO dispatchNext(@PathVariable Long requestId) {
        log.info("api.enter requests.dispatch-next requestId={}", requestId);
        DispatchResultDTO response = requestService.dispatchNextTwenty(requestId);
        log.info("api.exit requests.dispatch-next notifiedFrom={} notifiedTo={}", response.getNotifiedFrom(), response.getNotifiedTo());
        return response;
    }

    @GetMapping("/user/{userId}")
    public List<RequestSummaryDTO> getUserRequests(@PathVariable Long userId) {
        log.info("api.enter requests.user-history userId={}", userId);
        List<RequestSummaryDTO> requests = requestService.getUserRequestHistory(userId);
        log.info("api.exit requests.user-history count={}", requests.size());
        return requests;
    }

    @GetMapping("/user/{userId}/responses")
    public List<ResponseRecordDTO> getUserResponses(@PathVariable Long userId) {
        log.info("api.enter requests.user-responses userId={}", userId);
        List<ResponseRecordDTO> responses = requestService.getUserResponses(userId);
        log.info("api.exit requests.user-responses count={}", responses.size());
        return responses;
    }

    @GetMapping("/user/{userId}/active")
    public ActiveRequestStatusDTO hasActive(@PathVariable Long userId) {
        log.info("api.enter requests.user-active userId={}", userId);
        boolean active = requestService.hasActiveRequestForUser(userId);
        boolean createdToday = requestService.hasRootRequestTodayForUser(userId);
        ActiveRequestStatusDTO response = new ActiveRequestStatusDTO(active, createdToday);
        log.info("api.exit requests.user-active active={}", response.isActive());
        return response;
    }
}
