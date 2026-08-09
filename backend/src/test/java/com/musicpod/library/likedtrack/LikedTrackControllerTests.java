package com.musicpod.library.likedtrack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.jwt;

import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.data.domain.Page;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.musicpod.common.api.PageResponse;
import com.musicpod.common.exception.ResourceNotFoundException;

@SpringBootTest
@AutoConfigureMockMvc
class LikedTrackControllerTests {

    private static final String BASE_URL =
            "/api/v1/me/liked-tracks";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LikedTrackService likedTrackService;

    private UUID userId;
    private UUID trackId;

    @BeforeEach
    void setUp() {

        userId =
                UUID.randomUUID();

        trackId =
                UUID.randomUUID();
    }

    /*
     * PUT /api/v1/me/liked-tracks/{trackId}
     *
     * Authenticated user can like a track.
     */
    @Test
    void authenticatedUserCanLikeTrack()
            throws Exception {

        Instant likedAt =
                Instant.parse(
                        "2026-08-09T12:00:00Z"
                );

        LikedTrackResponse response =
                new LikedTrackResponse(
                        null,
                        likedAt
                );

        when(
                likedTrackService.like(
                        userId,
                        trackId
                )
        ).thenReturn(
                response
        );

        mockMvc.perform(
                        put(
                                BASE_URL
                                        + "/"
                                        + trackId
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath(
                                "$.likedAt"
                        ).value(
                                likedAt.toString()
                        )
                );

        verify(
                likedTrackService
        ).like(
                userId,
                trackId
        );
    }

    /*
     * PUT is idempotent at our API/service contract.
     *
     * Repeating the same request should continue
     * succeeding.
     */
    @Test
    void likingSameTrackTwiceRemainsSuccessful()
            throws Exception {

        LikedTrackResponse response =
                new LikedTrackResponse(
                        null,
                        Instant.now()
                );

        when(
                likedTrackService.like(
                        userId,
                        trackId
                )
        ).thenReturn(
                response
        );

        mockMvc.perform(
                        put(
                                BASE_URL
                                        + "/"
                                        + trackId
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        put(
                                BASE_URL
                                        + "/"
                                        + trackId
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                likedTrackService,
                times(2)
        ).like(
                userId,
                trackId
        );
    }

    /*
     * DELETE /api/v1/me/liked-tracks/{trackId}
     */
    @Test
    void authenticatedUserCanUnlikeTrack()
            throws Exception {

        mockMvc.perform(
                        delete(
                                BASE_URL
                                        + "/"
                                        + trackId
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                likedTrackService
        ).unlike(
                userId,
                trackId
        );
    }

    /*
     * DELETE is idempotent.
     *
     * Deleting a relationship that has already
     * been deleted must remain successful.
     */
    @Test
    void unlikeIsIdempotent()
            throws Exception {

        mockMvc.perform(
                        delete(
                                BASE_URL
                                        + "/"
                                        + trackId
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        delete(
                                BASE_URL
                                        + "/"
                                        + trackId
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                likedTrackService,
                times(2)
        ).unlike(
                userId,
                trackId
        );
    }

    /*
     * GET /api/v1/me/liked-tracks
     *
     * Default pagination should be:
     *
     * page = 0
     * size = 20
     */
    @Test
    void returnsAuthenticatedUsersLikedTracks()
            throws Exception {

        PageResponse<LikedTrackResponse> response =
                PageResponse.from(
                        Page.empty()
                );

        when(
                likedTrackService.getLikedTracks(
                        userId,
                        0,
                        20
                )
        ).thenReturn(
                response
        );

        mockMvc.perform(
                        get(
                                BASE_URL
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                likedTrackService
        ).getLikedTracks(
                userId,
                0,
                20
        );
    }

    /*
     * Explicit page/size parameters must be
     * forwarded to the service.
     */
    @Test
    void usesRequestedPagination()
            throws Exception {

        PageResponse<LikedTrackResponse> response =
                PageResponse.from(
                        Page.empty()
                );

        when(
                likedTrackService.getLikedTracks(
                        userId,
                        2,
                        5
                )
        ).thenReturn(
                response
        );

        mockMvc.perform(
                        get(
                                BASE_URL
                        )
                                .param(
                                        "page",
                                        "2"
                                )
                                .param(
                                        "size",
                                        "5"
                                )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                likedTrackService
        ).getLikedTracks(
                userId,
                2,
                5
        );
    }

    /*
     * Critical security contract:
     *
     * userId comes from JWT subject.
     *
     * Different JWT subjects must therefore
     * result in different userIds being passed
     * to the service.
     */
    @Test
    void differentJwtSubjectsUseDifferentUsers()
            throws Exception {

        UUID firstUserId =
                UUID.randomUUID();

        UUID secondUserId =
                UUID.randomUUID();

        PageResponse<LikedTrackResponse> response =
                PageResponse.from(
                        Page.empty()
                );

        when(
                likedTrackService.getLikedTracks(
                        firstUserId,
                        0,
                        20
                )
        ).thenReturn(
                response
        );

        when(
                likedTrackService.getLikedTracks(
                        secondUserId,
                        0,
                        20
                )
        ).thenReturn(
                response
        );

        mockMvc.perform(
                        get(
                                BASE_URL
                        )
                                .with(
                                        authenticatedUser(
                                                firstUserId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                );

        mockMvc.perform(
                        get(
                                BASE_URL
                        )
                                .with(
                                        authenticatedUser(
                                                secondUserId
                                        )
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(
                likedTrackService
        ).getLikedTracks(
                firstUserId,
                0,
                20
        );

        verify(
                likedTrackService
        ).getLikedTracks(
                secondUserId,
                0,
                20
        );
    }

    /*
     * Service-layer ResourceNotFoundException
     * should become HTTP 404 through our
     * existing exception handling.
     */
    @Test
    void unknownTrackReturnsNotFound()
            throws Exception {

        when(
                likedTrackService.like(
                        userId,
                        trackId
                )
        ).thenThrow(
                new ResourceNotFoundException(
                        "Track not found: "
                                + trackId
                )
        );

        mockMvc.perform(
                        put(
                                BASE_URL
                                        + "/"
                                        + trackId
                        )
                                .with(
                                        authenticatedUser(
                                                userId
                                        )
                                )
                )
                .andExpect(
                        status().isNotFound()
                );

        verify(
                likedTrackService
        ).like(
                userId,
                trackId
        );
    }

    /*
     * GET requires authentication.
     */
    @Test
    void likedTracksRequireAuthentication()
            throws Exception {

        mockMvc.perform(
                        get(
                                BASE_URL
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verifyNoInteractions(
                likedTrackService
        );
    }

    /*
     * PUT requires authentication.
     */
    @Test
    void likingTrackRequiresAuthentication()
            throws Exception {

        mockMvc.perform(
                        put(
                                BASE_URL
                                        + "/"
                                        + trackId
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verify(
                likedTrackService,
                never()
        ).like(
                any(),
                any()
        );
    }

    /*
     * DELETE requires authentication.
     */
    @Test
    void unlikeRequiresAuthentication()
            throws Exception {

        mockMvc.perform(
                        delete(
                                BASE_URL
                                        + "/"
                                        + trackId
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verify(
                likedTrackService,
                never()
        ).unlike(
                any(),
                any()
        );
    }

    /*
     * Creates the same type of Jwt principal
     * our controller receives in production:
     *
     * @AuthenticationPrincipal Jwt jwt
     *
     * Controller then reads:
     *
     * jwt.getSubject()
     */
    private RequestPostProcessor authenticatedUser(
            UUID userId) {

        return jwt()
                .jwt(builder ->
                        builder.subject(
                                userId.toString()
                        )
                );
    }
}