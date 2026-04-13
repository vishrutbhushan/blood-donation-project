package com.hemo.backend.controller;

import com.hemo.backend.dto.UserDTO;
import com.hemo.backend.entity.User;
import com.hemo.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // ✅ Create User
    @PostMapping
    public User createUser(@Valid @RequestBody UserDTO dto) {
        return userService.createUser(dto);
    }

    // ✅ Get User by ID
    @GetMapping("/{userId}")
    public User getUser(@PathVariable Long userId) {
        return userService.getUserById(userId);
    }

    @GetMapping("/phone/{phone}")
    public User getUserByPhone(@PathVariable String phone) {
        return userService.getUserByPhone(phone);
    }

    @PostMapping("/get-or-create")
    public User getOrCreateUser(@Valid @RequestBody UserDTO dto) {
        return userService.getOrCreateUser(dto);
    }
}
