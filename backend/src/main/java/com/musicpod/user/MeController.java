package com.musicpod.user;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeController {

    private final UserService userService;

    public MeController(
            UserService userService) {

        this.userService = userService;
    }

    @GetMapping("/api/v1/me")
    public UserResponse getCurrentUser(
            @AuthenticationPrincipal
            Jwt jwt) {

        UUID userId =
                UUID.fromString(
                        jwt.getSubject()
                );

        return userService
                .getById(userId);
    }
}