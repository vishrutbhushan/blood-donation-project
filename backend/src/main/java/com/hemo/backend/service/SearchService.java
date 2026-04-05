package com.hemo.backend.service;

import com.hemo.backend.entity.Search;
import com.hemo.backend.entity.User;
import com.hemo.backend.exception.ResourceNotFoundException;
import com.hemo.backend.repository.SearchRepository;
import com.hemo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final SearchRepository searchRepository;
    private final UserRepository userRepository;

    public Search createSearch(Long userId, Search search) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        search.setUser(user);

        return searchRepository.save(search);
    }

    public Search getSearchById(Long searchId) {
        return searchRepository.findById(searchId)
                .orElseThrow(() -> new ResourceNotFoundException("Search not found"));
    }
}
