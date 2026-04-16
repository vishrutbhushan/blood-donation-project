package com.hemo.backend.controller;

import com.hemo.backend.dto.SearchCreateRequestDTO;
import com.hemo.backend.dto.SearchResponseDTO;
import com.hemo.backend.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/searches")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;

    @PostMapping("/{userId}")
    public SearchResponseDTO createSearch(@PathVariable Long userId,
                              @RequestBody SearchCreateRequestDTO payload) {
        log.info("api.enter searches.create userId={}", userId);
        SearchResponseDTO response = searchService.createSearch(userId, payload);
        log.info("api.exit searches.create searchId={}", response.getSearchId());
        return response;
    }
}
