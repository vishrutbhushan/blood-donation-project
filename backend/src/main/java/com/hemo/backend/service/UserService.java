package com.hemo.backend.service;

import com.hemo.backend.entity.User;
import com.hemo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User getOrCreateByAbha(String abhaId) {
        return userRepository.findByAbhaId(abhaId)
                .orElseGet(() -> {
                    User user = new User();
                    user.setAbhaId(abhaId);
                    user.setName("Demo User");
                    user.setPhone(abhaId.substring(0, 10));
                    return userRepository.save(user);
                });
    }
}
