package com.musicpod.auth;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.musicpod.user.UserResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthService authService) {

        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(
            @Valid
            @RequestBody
            RegisterRequest request,

            UriComponentsBuilder uriBuilder) {

        UserResponse user =
                authService.register(request);

        URI location = uriBuilder
                .path("/api/v1/users/{id}")
                .buildAndExpand(user.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(user);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid
            @RequestBody
            LoginRequest request) {

        return authService.login(request);
    }
}