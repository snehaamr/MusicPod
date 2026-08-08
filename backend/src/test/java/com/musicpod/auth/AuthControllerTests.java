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
import org.springframework.test.web.servlet.MvcResult;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.jayway.jsonpath.JsonPath;
import com.musicpod.user.UserAccount;
import com.musicpod.user.UserRepository;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
            new PostgreSQLContainer<>(
                    "postgres:17-alpine"
            )
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
                        .value(
                                "listener@example.com"
                        )
        )
        .andExpect(
                jsonPath("$.displayName")
                        .value(
                                "Music Listener"
                        )
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
                savedUser
                        .getPasswordHash()
                        .equals(
                                "musicpod123!"
                        )
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
                        .value(
                                "listener@example.com"
                        )
        );
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase()
            throws Exception {

        createUser();

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
                        .value(
                                "Email must be valid"
                        )
        )
        .andExpect(
                jsonPath("$.fieldErrors.password")
                        .value(
                                "Password must be between 8 and 72 characters"
                        )
        )
        .andExpect(
                jsonPath("$.fieldErrors.displayName")
                        .value(
                                "Display name is required"
                        )
        );
    }

    @Test
    void logsInWithValidCredentials()
            throws Exception {

        createUser();

        mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content("""
                                {
                                  "email": "listener@example.com",
                                  "password": "musicpod123!"
                                }
                                """)
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.accessToken")
                        .isNotEmpty()
        )
        .andExpect(
                jsonPath("$.tokenType")
                        .value("Bearer")
        )
        .andExpect(
                jsonPath("$.expiresInSeconds")
                        .value(3600)
        )
        .andExpect(
                jsonPath("$.user.email")
                        .value(
                                "listener@example.com"
                        )
        );
    }

    @Test
    void rejectsWrongPassword()
            throws Exception {

        createUser();

        mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content("""
                                {
                                  "email": "listener@example.com",
                                  "password": "wrong-password"
                                }
                                """)
        )
        .andExpect(status().isUnauthorized())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Invalid email or password"
                        )
        );
    }

    @Test
    void rejectsUnknownEmail()
            throws Exception {

        mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content("""
                                {
                                  "email": "unknown@example.com",
                                  "password": "musicpod123!"
                                }
                                """)
        )
        .andExpect(status().isUnauthorized())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Invalid email or password"
                        )
        );
    }

    @Test
    void meRequiresAuthentication()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/me")
        )
        .andExpect(
                status().isUnauthorized()
        );
    }

    @Test
    void loginTokenCanAccessMe()
            throws Exception {

        createUser();

        MvcResult loginResult =
                mockMvc.perform(
                        post("/api/v1/auth/login")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content("""
                                        {
                                          "email": "listener@example.com",
                                          "password": "musicpod123!"
                                        }
                                        """)
                )
                .andExpect(
                        status().isOk()
                )
                .andReturn();

        String responseBody =
                loginResult
                        .getResponse()
                        .getContentAsString();

        String accessToken =
                JsonPath.read(
                        responseBody,
                        "$.accessToken"
                );

        mockMvc.perform(
                get("/api/v1/me")
                        .header(
                                "Authorization",
                                "Bearer "
                                        + accessToken
                        )
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.email")
                        .value(
                                "listener@example.com"
                        )
        )
        .andExpect(
                jsonPath("$.displayName")
                        .value(
                                "Music Listener"
                        )
        );
    }

    @Test
    void rejectsInvalidBearerToken()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/me")
                        .header(
                                "Authorization",
                                "Bearer not-a-real-jwt"
                        )
        )
        .andExpect(
                status().isUnauthorized()
        );
    }

    private UserAccount createUser() {

        String passwordHash =
                passwordEncoder.encode(
                        "musicpod123!"
                );

        return userRepository.save(
                new UserAccount(
                        "listener@example.com",
                        passwordHash,
                        "Music Listener"
                )
        );
    }
}