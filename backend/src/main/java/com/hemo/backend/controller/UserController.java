package com.hemo.backend.controller;

import com.hemo.backend.dto.UserDTO;
import com.hemo.backend.entity.User;
import com.hemo.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/get-or-create")
    public User getOrCreateUser(@Valid @RequestBody UserDTO dto) {
        User user = userService.getOrCreateByAbha(dto.getAbhaId());
        log.info("users get-or-create resolved userId={}", user.getUserId());
        return user;
    }
}
