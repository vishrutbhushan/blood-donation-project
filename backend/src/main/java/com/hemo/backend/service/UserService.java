package com.hemo.backend.service;


import com.hemo.backend.dto.UserDTO;
import com.hemo.backend.entity.User;
import com.hemo.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(UserDTO dto) {

        User user = new User();
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());

        return userRepository.save(user);
    }

    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByPhone(String phone) {
        return userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getOrCreateUser(UserDTO dto) {
        return userRepository.findByPhone(dto.getPhone())
                .orElseGet(() -> createUser(dto));
    }
}
