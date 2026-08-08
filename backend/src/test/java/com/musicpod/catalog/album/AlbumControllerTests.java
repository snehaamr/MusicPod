package com.musicpod.catalog.album;

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

import com.musicpod.catalog.artist.Artist;
import com.musicpod.catalog.artist.ArtistRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class AlbumControllerTests {

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

    @BeforeEach
    void cleanDatabase() {

        albumRepository.deleteAll();
        artistRepository.deleteAll();
    }

    @Test
    void createsAlbumForArtist() throws Exception {

        Artist artist = artistRepository.save(
                new Artist(
                        "Coldplay",
                        "British rock band",
                        null
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/artists/"
                                + artist.getId()
                                + "/albums"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Parachutes",
                          "releaseDate": "2000-07-10",
                          "coverImageUrl": "https://example.com/parachutes.jpg"
                        }
                        """)
        )
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(
                jsonPath("$.artistId")
                        .value(artist.getId().toString())
        )
        .andExpect(
                jsonPath("$.title")
                        .value("Parachutes")
        )
        .andExpect(
                jsonPath("$.releaseDate")
                        .value("2000-07-10")
        );
    }

    @Test
    void rejectsAlbumWithoutTitle() throws Exception {

        Artist artist = artistRepository.save(
                new Artist(
                        "Coldplay",
                        null,
                        null
                )
        );

        mockMvc.perform(
                post(
                        "/api/v1/artists/"
                                + artist.getId()
                                + "/albums"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "",
                          "releaseDate": "2000-07-10"
                        }
                        """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(
                jsonPath("$.fieldErrors.title")
                        .value("Album title is required")
        );
    }

    @Test
    void returnsNotFoundWhenArtistDoesNotExist()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/v1/artists/"
                                + "11111111-1111-1111-1111-111111111111"
                                + "/albums"
                )
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "title": "Unknown Album"
                        }
                        """)
        )
        .andExpect(status().isNotFound())
        .andExpect(
                jsonPath("$.message")
                        .value(
                                "Artist not found: "
                                + "11111111-1111-1111-1111-111111111111"
                        )
        );
    }

    @Test
    void returnsAlbumsForArtistWithPagination()
            throws Exception {

        Artist artist = artistRepository.save(
                new Artist(
                        "Coldplay",
                        null,
                        null
                )
        );

        albumRepository.save(
                new Album(
                        artist,
                        "Parachutes",
                        null,
                        null
                )
        );

        albumRepository.save(
                new Album(
                        artist,
                        "Ghost Stories",
                        null,
                        null
                )
        );

        albumRepository.save(
                new Album(
                        artist,
                        "X&Y",
                        null,
                        null
                )
        );

        mockMvc.perform(
                get(
                        "/api/v1/artists/"
                                + artist.getId()
                                + "/albums"
                )
                .param("page", "0")
                .param("size", "2")
        )
        .andExpect(status().isOk())
        .andExpect(
                jsonPath("$.content.length()")
                        .value(2)
        )
        .andExpect(
                jsonPath("$.totalElements")
                        .value(3)
        )
        .andExpect(
                jsonPath("$.totalPages")
                        .value(2)
        )

        // Alphabetical ordering.
        .andExpect(
                jsonPath("$.content[0].title")
                        .value("Ghost Stories")
        )
        .andExpect(
                jsonPath("$.content[1].title")
                        .value("Parachutes")
        );
    }
}