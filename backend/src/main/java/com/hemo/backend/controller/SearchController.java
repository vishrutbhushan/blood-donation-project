package com.hemo.backend.controller;

import com.hemo.backend.entity.Search;
import com.hemo.backend.service.SearchService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/searches")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    // ✅ Create Search
    @PostMapping("/{userId}")
    public Search createSearch(@PathVariable Long userId,
                              @RequestBody Search search) {
        return searchService.createSearch(userId, search);
    }

    // ✅ Get Search by ID
    @GetMapping("/{searchId}")
    public Search getSearch(@PathVariable Long searchId) {
        return searchService.getSearchById(searchId);
    }
}
