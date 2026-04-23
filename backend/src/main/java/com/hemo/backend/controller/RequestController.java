package com.hemo.backend.controller;

import com.hemo.backend.dto.ActiveRequestStatusDTO;
import com.hemo.backend.dto.DonorSearchSummaryDTO;
import com.hemo.backend.dto.RequestDTO;
import com.hemo.backend.dto.RequestSummaryDTO;
import com.hemo.backend.dto.ResponseRecordDTO;
import com.hemo.backend.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    @PostMapping("/{searchId}")
    public RequestSummaryDTO createRequest(@PathVariable Long searchId,
                                @Valid @RequestBody RequestDTO dto) {
        RequestSummaryDTO response = requestService.createRequest(searchId, dto);
        return response;
    }

    @PostMapping("/{requestId}/re-request")
    public RequestSummaryDTO reRequest(@PathVariable Long requestId) {
        RequestSummaryDTO response = requestService.reRequest(requestId);
        return response;
    }

    @GetMapping("/{requestId}/re-request-preview")
    public DonorSearchSummaryDTO reRequestPreview(@PathVariable Long requestId) {
        DonorSearchSummaryDTO preview = requestService.previewReRequest(requestId);
        return preview;
    }

    @GetMapping("/user/{userId}")
    public List<RequestSummaryDTO> getUserRequests(@PathVariable Long userId) {
        List<RequestSummaryDTO> requests = requestService.getUserRequestHistory(userId);
        return requests;
    }

    @GetMapping("/user/{userId}/responses")
    public List<ResponseRecordDTO> getUserResponses(@PathVariable Long userId) {
        List<ResponseRecordDTO> responses = requestService.getUserResponses(userId);
        return responses;
    }

    @GetMapping("/user/{userId}/active")
    public ActiveRequestStatusDTO hasActive(@PathVariable Long userId) {
        boolean active = requestService.hasActiveRequestForUser(userId);
        boolean createdToday = requestService.hasRootRequestTodayForUser(userId);
        ActiveRequestStatusDTO response = new ActiveRequestStatusDTO(active, createdToday);
        return response;
    }
}
