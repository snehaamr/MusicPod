package com.musicpod.auth;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public UUID userId() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "Authenticated user is required"
            );
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal instanceof Jwt jwt)) {

            throw new IllegalStateException(
                    "Authenticated principal is not a JWT"
            );
        }

        String subject =
                jwt.getSubject();

        if (subject == null
                || subject.isBlank()) {

            throw new IllegalStateException(
                    "JWT subject is missing"
            );
        }

        try {

            return UUID.fromString(subject);

        } catch (IllegalArgumentException exception) {

            throw new IllegalStateException(
                    "JWT subject is not a valid MusicPod user UUID",
                    exception
            );
        }
    }
}