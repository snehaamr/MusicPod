package com.musicpod.catalog.artist;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ArtistControllerTests {

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

    @BeforeEach
    void cleanDatabase() {
        artistRepository.deleteAll();
    }

    @Test
    void createsArtist() throws Exception {

        mockMvc.perform(
                post("/api/v1/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Radiohead",
                                  "bio": "English rock band",
                                  "imageUrl": "https://example.com/radiohead.jpg"
                                }
                                """)
        )
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.name").value("Radiohead"))
        .andExpect(jsonPath("$.bio").value("English rock band"));
    }

    @Test
    void rejectsArtistWithoutName() throws Exception {

        mockMvc.perform(
                post("/api/v1/artists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "bio": "Missing name"
                                }
                                """)
        )
        .andExpect(status().isBadRequest())
        .andExpect(
                jsonPath("$.message")
                        .value("Request validation failed")
        )
        .andExpect(
                jsonPath("$.fieldErrors.name")
                        .value("Artist name is required")
        );
    }

    @Test
    void returnsNotFoundForUnknownArtist() throws Exception {

        mockMvc.perform(
                get(
                    "/api/v1/artists/"
                    + "11111111-1111-1111-1111-111111111111"
                )
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
    void returnsPaginatedArtists() throws Exception {

        artistRepository.save(
                new Artist(
                        "Radiohead",
                        null,
                        null
                )
        );

        artistRepository.save(
                new Artist(
                        "Coldplay",
                        null,
                        null
                )
        );

        artistRepository.save(
                new Artist(
                        "Muse",
                        null,
                        null
                )
        );

        mockMvc.perform(
                get("/api/v1/artists")
                        .param("page", "0")
                        .param("size", "2")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.first").value(true))
        .andExpect(jsonPath("$.last").value(false))

        // Sorted alphabetically.
        .andExpect(jsonPath("$.content[0].name").value("Coldplay"))
        .andExpect(jsonPath("$.content[1].name").value("Muse"));
    }
}