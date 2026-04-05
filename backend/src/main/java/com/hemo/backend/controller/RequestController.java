package com.hemo.backend.controller;

import com.hemo.backend.dto.RequestDTO;
import com.hemo.backend.entity.Request;
import com.hemo.backend.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/requests")
@RequiredArgsConstructor
public class RequestController {

    private final RequestService requestService;

    // ✅ Create Request
    @PostMapping("/{searchId}")
    public Request createRequest(@PathVariable Long searchId,
                                @Valid @RequestBody RequestDTO dto) {
        return requestService.createRequest(searchId, dto);
    }

    // ✅ Retry Request (Self FK)
    @PostMapping("/retry/{requestId}")
    public Request retryRequest(@PathVariable Long requestId) {
        return requestService.retryRequest(requestId);
    }

    // ✅ Get Request History (IMPORTANT)
    @GetMapping("/user/{userId}")
    public List<Map<String, Object>> getUserRequests(@PathVariable Long userId) {
        return requestService.getUserRequestHistory(userId);
    }
}
