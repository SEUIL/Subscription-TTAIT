package com.example.demo.user.controller;

import com.example.demo.common.web.CurrentUser;
import com.example.demo.user.dto.UserProfileRequest;
import com.example.demo.user.dto.UserProfileResponse;
import com.example.demo.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public UserProfileResponse me() {
        return userProfileService.getMe(CurrentUser.id());
    }

    @PutMapping("/profile")
    public UserProfileResponse upsertProfile(@Valid @RequestBody UserProfileRequest request) {
        return userProfileService.upsertProfile(CurrentUser.id(), request);
    }
}
