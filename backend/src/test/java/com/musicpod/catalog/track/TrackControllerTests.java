package com.musicpod.catalog.track;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.musicpod.catalog.album.Album;
import com.musicpod.catalog.album.AlbumRepository;
import com.musicpod.catalog.artist.Artist;
import com.musicpod.catalog.artist.ArtistRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class TrackControllerTests {

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
    private ArtistRepository artistRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private TrackRepository trackRepository;

    private Album album;

    @BeforeEach
    void setUp() {

        trackRepository.deleteAll();
        albumRepository.deleteAll();
        artistRepository.deleteAll();

        Artist artist = artistRepository.save(
                new Artist(
                        "Coldplay",
                        "British rock band",
                        null
                )
        );

        album = albumRepository.save(
                new Album(
                        artist,
                        "A Rush of Blood to the Head",
                        null,
                        null
                )
        );
    }

    @Test
    void createsTrackForAlbum() throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/albums/"
                                + album.getId()
                                + "/tracks"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Clocks",
                          "trackNumber": 5,
                          "durationMs": 307000,
                          "explicit": false
                        }
                        """)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(
                jsonPath("$.albumId")
                        .value(album.getId().toString())
        )
        .andExpect(
                jsonPath("$.title")
                        .value("Clocks")
        )
        .andExpect(
                jsonPath("$.trackNumber")
                        .value(5)
        )
        .andExpect(
                jsonPath("$.durationMs")
                        .value(307000)
        );
    }

    @Test
    void rejectsDuplicateTrackNumber()
            throws Exception {

        trackRepository.save(
                new Track(
                        album,
                        "Clocks",
                        5,
                        307000,
                        false
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/albums/"
                                + album.getId()
                                + "/tracks"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Another Song",
                          "trackNumber": 5,
                          "durationMs": 250000,
                          "explicit": false
                        }
                        """)
        )
        .andExpect(status().isConflict())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Track number 5 already exists for album "
                                        + album.getId()
                        )
        );
    }

    @Test
    void rejectsInvalidTrackData()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/albums/"
                                + album.getId()
                                + "/tracks"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Bad Track",
                          "trackNumber": 0,
                          "durationMs": -100,
                          "explicit": false
                        }
                        """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(
                jsonPath("$.fieldErrors.trackNumber")
                        .value(
                                "Track number must be greater than 0"
                        )
        )
        .andExpect(
                jsonPath("$.fieldErrors.durationMs")
                        .value(
                                "Duration must be greater than 0"
                        )
        );
    }

    @Test
    void returnsNotFoundForUnknownAlbum()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/albums/"
                                + "11111111-1111-1111-1111-111111111111"
                                + "/tracks"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Unknown Track",
                          "trackNumber": 1,
                          "durationMs": 200000,
                          "explicit": false
                        }
                        """)
        )
        .andExpect(status().isNotFound())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Album not found: "
                                + "11111111-1111-1111-1111-111111111111"
                        )
        );
    }

    @Test
    void returnsTracksInTrackNumberOrder()
            throws Exception {

        trackRepository.save(
                new Track(
                        album,
                        "Third",
                        3,
                        200000,
                        false
                )
        );

        trackRepository.save(
                new Track(
                        album,
                        "First",
                        1,
                        200000,
                        false
                )
        );

        trackRepository.save(
                new Track(
                        album,
                        "Second",
                        2,
                        200000,
                        false
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/albums/"
                                + album.getId()
                                + "/tracks"
                )
                .param("page", "0")
                .param("size", "10")
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.totalElements")
                        .value(3)
        )
        .andExpect(
                jsonPath("$.content[0].trackNumber")
                        .value(1)
        )
        .andExpect(
                jsonPath("$.content[1].trackNumber")
                        .value(2)
        )
        .andExpect(
                jsonPath("$.content[2].trackNumber")
                        .value(3)
        );
    }
}