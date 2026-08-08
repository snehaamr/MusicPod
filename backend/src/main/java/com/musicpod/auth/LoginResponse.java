package com.musicpod.auth;

import com.musicpod.user.UserResponse;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        UserResponse user
) {
}