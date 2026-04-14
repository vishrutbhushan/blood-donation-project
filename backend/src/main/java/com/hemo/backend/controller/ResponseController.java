package com.hemo.backend.controller;

import com.hemo.backend.dto.ResponseRecordDTO;
import com.hemo.backend.dto.ResponseDTO;
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

    @PostMapping("/{requestId}")
    public ResponseRecordDTO addResponse(@PathVariable Long requestId,
                               @Valid @RequestBody ResponseDTO dto) {
        return responseService.addResponse(requestId, dto);
    }

    @GetMapping("/{requestId}")
    public List<ResponseRecordDTO> getResponses(@PathVariable Long requestId) {
        return responseService.getResponsesByRequest(requestId);
    }
}
