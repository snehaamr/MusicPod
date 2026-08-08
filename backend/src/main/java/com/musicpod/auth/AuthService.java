package com.musicpod.auth;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.common.exception.ResourceConflictException;
import com.musicpod.user.UserAccount;
import com.musicpod.user.UserRepository;
import com.musicpod.user.UserResponse;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(
            RegisterRequest request) {

        String normalizedEmail =
                normalizeEmail(request.email());

        if (userRepository
                .existsByEmailIgnoreCase(normalizedEmail)) {

            throw duplicateEmailException();
        }

        String passwordHash =
                passwordEncoder.encode(
                        request.password()
                );

        UserAccount user = new UserAccount(
                normalizedEmail,
                passwordHash,
                request.displayName().trim()
        );

        try {

            UserAccount savedUser =
                    userRepository.saveAndFlush(user);

            return UserResponse.from(savedUser);

        } catch (DataIntegrityViolationException exception) {

            /*
             * The exists check above improves the common-case
             * error message, but it cannot eliminate races.
             *
             * Two concurrent registrations could both check
             * before either inserts.
             *
             * PostgreSQL's unique index remains the final
             * correctness boundary.
             */

            throw duplicateEmailException();
        }
    }

    private String normalizeEmail(String email) {

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
}