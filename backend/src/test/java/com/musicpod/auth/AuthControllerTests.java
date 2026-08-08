package com.musicpod.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.musicpod.user.UserAccount;
import com.musicpod.user.UserRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("musicpod")
                    .withUsername("musicpod")
                    .withPassword("musicpod");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void registersUserAndHashesPassword()
            throws Exception {

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content("""
                                {
                                  "email": "listener@example.com",
                                  "password": "musicpod123!",
                                  "displayName": "Music Listener"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(
                jsonPath("$.id")
                        .isNotEmpty()
        )
        .andExpect(
                jsonPath("$.email")
                        .value("listener@example.com")
        )
        .andExpect(
                jsonPath("$.displayName")
                        .value("Music Listener")
        )
        .andExpect(
                jsonPath("$.password")
                        .doesNotExist()
        )
        .andExpect(
                jsonPath("$.passwordHash")
                        .doesNotExist()
        );

        UserAccount savedUser =
                userRepository
                        .findByEmailIgnoreCase(
                                "listener@example.com"
                        )
                        .orElseThrow();

        assertFalse(
                savedUser.getPasswordHash()
                        .equals("musicpod123!")
        );

        assertTrue(
                passwordEncoder.matches(
                        "musicpod123!",
                        savedUser.getPasswordHash()
                )
        );
    }

    @Test
    void normalizesEmailBeforeSaving()
            throws Exception {

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content("""
                                {
                                  "email": "  Listener@Example.COM  ",
                                  "password": "musicpod123!",
                                  "displayName": "Listener"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(
                jsonPath("$.email")
                        .value("listener@example.com")
        );
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase()
            throws Exception {

        String passwordHash =
                passwordEncoder.encode(
                        "existingPassword123!"
                );

        userRepository.save(
                new UserAccount(
                        "listener@example.com",
                        passwordHash,
                        "Existing Listener"
                )
        );

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content("""
                                {
                                  "email": "LISTENER@EXAMPLE.COM",
                                  "password": "differentPassword123!",
                                  "displayName": "Another Listener"
                                }
                                """)
        )
        .andExpect(status().isConflict())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "An account with this email already exists"
                        )
        );
    }

    @Test
    void rejectsInvalidRegistration()
            throws Exception {

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content("""
                                {
                                  "email": "not-an-email",
                                  "password": "short",
                                  "displayName": ""
                                }
                                """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(
                jsonPath("$.fieldErrors.email")
                        .value("Email must be valid")
        )
        .andExpect(
                jsonPath("$.fieldErrors.password")
                        .value(
                                "Password must be between 8 and 72 characters"
                        )
        )
        .andExpect(
                jsonPath("$.fieldErrors.displayName")
                        .value("Display name is required")
        );
    }
}