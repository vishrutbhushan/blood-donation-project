package com.hemo.backend.controller;

import com.hemo.backend.dto.SearchCreateRequestDTO;
import com.hemo.backend.dto.SearchResponseDTO;
import com.hemo.backend.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/searches")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/{userId}")
    public SearchResponseDTO createSearch(@PathVariable Long userId,
                              @RequestBody SearchCreateRequestDTO payload) {
        return searchService.createSearch(userId, payload);
    }
}
