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

    @PostMapping("/get-or-create")
    public User getOrCreateUser(@Valid @RequestBody UserDTO dto) {
        return userService.getOrCreateByAbha(dto.getAbhaId());
    }
}
