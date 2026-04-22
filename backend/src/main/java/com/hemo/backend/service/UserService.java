package com.hemo.backend.service;

import com.hemo.backend.entity.User;
import com.hemo.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Value("${app.fixed-requestor-phone}")
    private String fixedRequestorPhone;

    public User getOrCreateByAbha(String abhaId) {
        return userRepository.findByAbhaId(abhaId)
                .orElseGet(() -> {
                    User user = new User();
                    user.setAbhaId(abhaId);
                    user.setName("Requestor");
                    user.setPhone(fixedRequestorPhone);
                    return userRepository.save(user);
                });
    }
}
