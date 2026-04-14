package com.hemo.backend.controller;

import com.hemo.backend.dto.ActiveRequestStatusDTO;
import com.hemo.backend.dto.DispatchResultDTO;
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
        return requestService.createRequest(searchId, dto);
    }

    @PostMapping("/{requestId}/re-request")
    public RequestSummaryDTO reRequest(@PathVariable Long requestId) {
        return requestService.reRequest(requestId);
    }

    @PostMapping("/{requestId}/dispatch-next")
    public DispatchResultDTO dispatchNext(@PathVariable Long requestId) {
        return requestService.dispatchNextTwenty(requestId);
    }

    @GetMapping("/user/{userId}")
    public List<RequestSummaryDTO> getUserRequests(@PathVariable Long userId) {
        return requestService.getUserRequestHistory(userId);
    }

    @GetMapping("/user/{userId}/responses")
    public List<ResponseRecordDTO> getUserResponses(@PathVariable Long userId) {
        return requestService.getUserResponses(userId);
    }

    @GetMapping("/user/{userId}/active")
    public ActiveRequestStatusDTO hasActive(@PathVariable Long userId) {
        return new ActiveRequestStatusDTO(requestService.hasActiveRequest(userId));
    }
}
