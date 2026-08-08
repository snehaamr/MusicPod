package com.musicpod.library.playlist;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class PlaylistControllerTests {

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
    private PlaylistRepository playlistRepository;

    @Autowired
    private PlaylistTrackRepository playlistTrackRepository;

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

        playlistTrackRepository.deleteAll();
        playlistRepository.deleteAll();

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
    void playlistsRequireAuthentication()
            throws Exception {

        mockMvc.perform(
                get("/api/v1/me/playlists")
        )
        .andExpect(
                status().isUnauthorized()
        );
    }

    @Test
    void createsPlaylistForAuthenticatedUser()
            throws Exception {

        String token =
                loginAndGetToken(
                        userOne.getEmail()
                );

        mockMvc.perform(
                post("/api/v1/me/playlists")
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
                        .contentType(
                                MediaType.APPLICATION_JSON
                        )
                        .content("""
                                {
                                  "name": "Coding",
                                  "description": "Music for coding"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(
                jsonPath("$.id")
                        .isNotEmpty()
        )
        .andExpect(
                jsonPath("$.name")
                        .value("Coding")
        )
        .andExpect(
                jsonPath("$.description")
                        .value(
                                "Music for coding"
                        )
        );

        assertEquals(
                1,
                playlistRepository
                        .findByUser_Id(
                                userOne.getId(),
                                org.springframework.data.domain.PageRequest.of(
                                        0,
                                        10
                                )
                        )
                        .getTotalElements()
        );
    }

    @Test
    void usersCannotReadEachOthersPlaylists()
            throws Exception {

        Playlist playlist =
                playlistRepository.save(
                        new Playlist(
                                userOne,
                                "Private Playlist",
                                null
                        )
                );

        String userTwoToken =
                loginAndGetToken(
                        userTwo.getEmail()
                );

        mockMvc.perform(
                get(
                        "/api/v1/me/playlists/"
                                + playlist.getId()
                )
                .header(
                        "Authorization",
                        "Bearer "
                                + userTwoToken
                )
        )
        .andExpect(status().isNotFound())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Playlist not found: "
                                        + playlist.getId()
                        )
        );
    }

    @Test
    void addsTracksInOrder()
            throws Exception {

        Playlist playlist =
                playlistRepository.save(
                        new Playlist(
                                userOne,
                                "Coding",
                                null
                        )
                );

        String token =
                loginAndGetToken(
                        userOne.getEmail()
                );

        addTrack(
                token,
                playlist.getId(),
                trackOne.getId()
        );

        addTrack(
                token,
                playlist.getId(),
                trackTwo.getId()
        );

        mockMvc.perform(
                get(
                        "/api/v1/me/playlists/"
                                + playlist.getId()
                                + "/tracks"
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
                        "$.content[0].position"
                )
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
        )
        .andExpect(
                jsonPath(
                        "$.content[1].position"
                )
                        .value(2)
        )
        .andExpect(
                jsonPath(
                        "$.content[1].track.id"
                )
                        .value(
                                trackTwo
                                        .getId()
                                        .toString()
                        )
        );
    }

    @Test
    void addingSameTrackTwiceIsIdempotent()
            throws Exception {

        Playlist playlist =
                playlistRepository.save(
                        new Playlist(
                                userOne,
                                "Coding",
                                null
                        )
                );

        String token =
                loginAndGetToken(
                        userOne.getEmail()
                );

        addTrack(
                token,
                playlist.getId(),
                trackOne.getId()
        );

        addTrack(
                token,
                playlist.getId(),
                trackOne.getId()
        );

        assertEquals(
                1,
                playlistTrackRepository
                        .countByPlaylist_Id(
                                playlist.getId()
                        )
        );
    }

    @Test
    void removeTrackIsIdempotent()
            throws Exception {

        Playlist playlist =
                playlistRepository.save(
                        new Playlist(
                                userOne,
                                "Coding",
                                null
                        )
                );

        String token =
                loginAndGetToken(
                        userOne.getEmail()
                );

        addTrack(
                token,
                playlist.getId(),
                trackOne.getId()
        );

        String path =
                "/api/v1/me/playlists/"
                        + playlist.getId()
                        + "/tracks/"
                        + trackOne.getId();

        mockMvc.perform(
                delete(path)
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
        )
        .andExpect(
                status().isNoContent()
        );

        mockMvc.perform(
                delete(path)
                        .header(
                                "Authorization",
                                "Bearer " + token
                        )
        )
        .andExpect(
                status().isNoContent()
        );

        assertEquals(
                0,
                playlistTrackRepository
                        .countByPlaylist_Id(
                                playlist.getId()
                        )
        );
    }

    @Test
    void deletingPlaylistRemovesPlaylistTracks()
            throws Exception {

        Playlist playlist =
                playlistRepository.save(
                        new Playlist(
                                userOne,
                                "Temporary",
                                null
                        )
                );

        String token =
                loginAndGetToken(
                        userOne.getEmail()
                );

        addTrack(
                token,
                playlist.getId(),
                trackOne.getId()
        );

        mockMvc.perform(
                delete(
                        "/api/v1/me/playlists/"
                                + playlist.getId()
                )
                .header(
                        "Authorization",
                        "Bearer " + token
                )
        )
        .andExpect(
                status().isNoContent()
        );

        assertEquals(
                0,
                playlistTrackRepository
                        .countByPlaylist_Id(
                                playlist.getId()
                        )
        );
    }

    private void addTrack(
            String token,
            java.util.UUID playlistId,
            java.util.UUID trackId)
            throws Exception {

        mockMvc.perform(
                put(
                        "/api/v1/me/playlists/"
                                + playlistId
                                + "/tracks/"
                                + trackId
                )
                .header(
                        "Authorization",
                        "Bearer " + token
                )
        )
        .andExpect(
                status().isOk()
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