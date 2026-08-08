package com.musicpod.playback;

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
import com.musicpod.catalog.album.Album;
import com.musicpod.catalog.album.AlbumRepository;
import com.musicpod.catalog.artist.Artist;
import com.musicpod.catalog.artist.ArtistRepository;
import com.musicpod.catalog.track.Track;
import com.musicpod.catalog.track.TrackRepository;
import com.musicpod.user.UserAccount;
import com.musicpod.user.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PlaybackControllerTests {

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
    private PlaybackEventRepository
            playbackEventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrackRepository trackRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private UserAccount userOne;
    private UserAccount userTwo;

    private Track trackOne;
    private Track trackTwo;

    @BeforeEach
    void setUp() {

        playbackEventRepository.deleteAll();

        trackRepository.deleteAll();
        albumRepository.deleteAll();
        artistRepository.deleteAll();

        userRepository.deleteAll();

        userOne = createUser(
                "listener1@example.com",
                "Listener One"
        );

        userTwo = createUser(
                "listener2@example.com",
                "Listener Two"
        );

        Artist artist =
                artistRepository.save(
                        new Artist(
                                "Coldplay",
                                null,
                                null
                        )
                );

        Album album =
                albumRepository.save(
                        new Album(
                                artist,
                                "A Rush of Blood to the Head",
                                null,
                                null
                        )
                );

        trackOne =
                trackRepository.save(
                        new Track(
                                album,
                                "Clocks",
                                1,
                                307000,
                                false
                        )
                );

        trackTwo =
                trackRepository.save(
                        new Track(
                                album,
                                "The Scientist",
                                2,
                                309000,
                                false
                        )
                );
    }

    @Test
    void playbackRequiresAuthentication()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/v1/me/recently-played"
                )
        )
        .andExpect(
                status().isUnauthorized()
        );
    }

    @Test
    void recordsPlaybackEvent()
            throws Exception {

        String token =
                loginAndGetToken(
                        userOne.getEmail()
                );

        mockMvc.perform(
                post(
                        "/api/v1/me/playback-events"
                )
                .header(
                        "Authorization",
                        "Bearer " + token
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .content(
                        """
                        {
                          "trackId": "%s",
                          "playedMs": 240000
                        }
                        """
                                .formatted(
                                        trackOne.getId()
                                )
                )
        )
        .andExpect(status().isCreated())
        .andExpect(
                jsonPath("$.id")
                        .isNotEmpty()
        )
        .andExpect(
                jsonPath("$.track.id")
                        .value(
                                trackOne
                                        .getId()
                                        .toString()
                        )
        )
        .andExpect(
                jsonPath("$.track.title")
                        .value("Clocks")
        )
        .andExpect(
                jsonPath("$.playedMs")
                        .value(240000)
        )
        .andExpect(
                jsonPath("$.playedAt")
                        .isNotEmpty()
        );

        assertEquals(
                1,
                playbackEventRepository
                        .countByUser_Id(
                                userOne.getId()
                        )
        );
    }

    @Test
    void sameTrackCanBePlayedMultipleTimes()
            throws Exception {

        String token =
                loginAndGetToken(
                        userOne.getEmail()
                );

        recordPlayback(
                token,
                trackOne.getId(),
                200000
        );

        recordPlayback(
                token,
                trackOne.getId(),
                250000
        );

        assertEquals(
                2,
                playbackEventRepository
                        .countByUser_Id(
                                userOne.getId()
                        )
        );
    }

    @Test
    void rejectsPlaybackLongerThanTrack()
            throws Exception {

        String token =
                loginAndGetToken(
                        userOne.getEmail()
                );

        mockMvc.perform(
                post(
                        "/api/v1/me/playback-events"
                )
                .header(
                        "Authorization",
                        "Bearer " + token
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .content(
                        """
                        {
                          "trackId": "%s",
                          "playedMs": 999999
                        }
                        """
                                .formatted(
                                        trackOne.getId()
                                )
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Played milliseconds must not exceed track duration"
                        )
        );
    }

    @Test
    void rejectsUnknownTrack()
            throws Exception {

        String token =
                loginAndGetToken(
                        userOne.getEmail()
                );

        mockMvc.perform(
                post(
                        "/api/v1/me/playback-events"
                )
                .header(
                        "Authorization",
                        "Bearer " + token
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .content("""
                        {
                          "trackId": "11111111-1111-1111-1111-111111111111",
                          "playedMs": 100000
                        }
                        """)
        )
        .andExpect(status().isNotFound())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Track not found: "
                                        + "11111111-1111-1111-1111-111111111111"
                        )
        );
    }

    @Test
    void recentlyPlayedReturnsNewestFirst()
            throws Exception {

        String token =
                loginAndGetToken(
                        userOne.getEmail()
                );

        recordPlayback(
                token,
                trackOne.getId(),
                200000
        );

        Thread.sleep(10);

        recordPlayback(
                token,
                trackTwo.getId(),
                200000
        );

        mockMvc.perform(
                get(
                        "/api/v1/me/recently-played"
                )
                .header(
                        "Authorization",
                        "Bearer " + token
                )
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.totalElements")
                        .value(2)
        )
        .andExpect(
                jsonPath(
                        "$.content[0].track.id"
                )
                        .value(
                                trackTwo
                                        .getId()
                                        .toString()
                        )
        )
        .andExpect(
                jsonPath(
                        "$.content[1].track.id"
                )
                        .value(
                                trackOne
                                        .getId()
                                        .toString()
                        )
        );
    }

    @Test
    void usersHaveIndependentPlaybackHistory()
            throws Exception {

        String userOneToken =
                loginAndGetToken(
                        userOne.getEmail()
                );

        String userTwoToken =
                loginAndGetToken(
                        userTwo.getEmail()
                );

        recordPlayback(
                userOneToken,
                trackOne.getId(),
                200000
        );

        recordPlayback(
                userTwoToken,
                trackTwo.getId(),
                200000
        );

        mockMvc.perform(
                get(
                        "/api/v1/me/recently-played"
                )
                .header(
                        "Authorization",
                        "Bearer "
                                + userOneToken
                )
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.totalElements")
                        .value(1)
        )
        .andExpect(
                jsonPath(
                        "$.content[0].track.id"
                )
                        .value(
                                trackOne
                                        .getId()
                                        .toString()
                        )
        );
    }

    private void recordPlayback(
            String token,
            java.util.UUID trackId,
            int playedMs)
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/me/playback-events"
                )
                .header(
                        "Authorization",
                        "Bearer " + token
                )
                .contentType(
                        MediaType.APPLICATION_JSON
                )
                .content(
                        """
                        {
                          "trackId": "%s",
                          "playedMs": %d
                        }
                        """
                                .formatted(
                                        trackId,
                                        playedMs
                                )
                )
        )
        .andExpect(
                status().isCreated()
        );
    }

    private UserAccount createUser(
            String email,
            String displayName) {

        return userRepository.save(
                new UserAccount(
                        email,
                        passwordEncoder.encode(
                                "musicpod123!"
                        ),
                        displayName
                )
        );
    }

    private String loginAndGetToken(
            String email)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                        post(
                                "/api/v1/auth/login"
                        )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content(
                                """
                                {
                                  "email": "%s",
                                  "password": "musicpod123!"
                                }
                                """
                                        .formatted(email)
                        )
                )
                .andExpect(status().isOk())
                .andReturn();

        return JsonPath.read(
                result
                        .getResponse()
                        .getContentAsString(),
                "$.accessToken"
        );
    }
}