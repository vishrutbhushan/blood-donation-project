package com.hemo.backend.controller;

import com.hemo.backend.dto.ResponseDTO;
import com.hemo.backend.entity.Response;
import com.hemo.backend.service.ResponseService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/responses")
@RequiredArgsConstructor
public class ResponseController {

    private final ResponseService responseService;

    // ✅ Add Donor Response
    @PostMapping("/{requestId}")
    public Response addResponse(@PathVariable Long requestId,
                               @Valid @RequestBody ResponseDTO dto) {
        return responseService.addResponse(requestId, dto);
    }

    // ✅ Get Responses for Request
    @GetMapping("/{requestId}")
    public List<Response> getResponses(@PathVariable Long requestId) {
        return responseService.getResponsesByRequest(requestId);
    }
}
