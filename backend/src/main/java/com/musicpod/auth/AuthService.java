package com.musicpod.auth;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.common.exception.AuthenticationFailedException;
import com.musicpod.common.exception.ResourceConflictException;
import com.musicpod.user.UserAccount;
import com.musicpod.user.UserRepository;
import com.musicpod.user.UserResponse;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public UserResponse register(
            RegisterRequest request) {

        String normalizedEmail =
                normalizeEmail(request.email());

        if (userRepository
                .existsByEmailIgnoreCase(
                        normalizedEmail
                )) {

            throw duplicateEmailException();
        }

        String passwordHash =
                passwordEncoder.encode(
                        request.password()
                );

        UserAccount user =
                new UserAccount(
                        normalizedEmail,
                        passwordHash,
                        request.displayName().trim()
                );

        try {

            UserAccount savedUser =
                    userRepository
                            .saveAndFlush(user);

            return UserResponse.from(
                    savedUser
            );

        } catch (
                DataIntegrityViolationException exception
        ) {

            throw duplicateEmailException();
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(
            LoginRequest request) {

        String normalizedEmail =
                normalizeEmail(request.email());

        UserAccount user =
                userRepository
                        .findByEmailIgnoreCase(
                                normalizedEmail
                        )
                        .orElseThrow(
                                this::invalidCredentialsException
                        );

        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        user.getPasswordHash()
                );

        if (!passwordMatches) {

            throw invalidCredentialsException();
        }

        String accessToken =
                tokenService
                        .createAccessToken(user);

        return new LoginResponse(
                accessToken,
                "Bearer",
                tokenService
                        .getAccessTokenTtlSeconds(),
                UserResponse.from(user)
        );
    }

    private String normalizeEmail(
            String email) {

        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private ResourceConflictException
            duplicateEmailException() {

        return new ResourceConflictException(
                "An account with this email already exists"
        );
    }

    private AuthenticationFailedException
            invalidCredentialsException() {

        return new AuthenticationFailedException(
                "Invalid email or password"
        );
    }
}