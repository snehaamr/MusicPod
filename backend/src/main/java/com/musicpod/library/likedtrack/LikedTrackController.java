package com.musicpod.library.likedtrack;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.musicpod.common.api.PageResponse;

@RestController
@RequestMapping(
        "/api/v1/me/liked-tracks"
)
public class LikedTrackController {

    private final LikedTrackService
            likedTrackService;

    public LikedTrackController(
            LikedTrackService likedTrackService) {

        this.likedTrackService =
                likedTrackService;
    }

    @PutMapping("/{trackId}")
    public LikedTrackResponse like(
            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID trackId) {

        UUID userId =
                getUserId(jwt);

        return likedTrackService.like(
                userId,
                trackId
        );
    }

    @DeleteMapping("/{trackId}")
    public void unlike(
            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID trackId) {

        UUID userId =
                getUserId(jwt);

        likedTrackService.unlike(
                userId,
                trackId
        );
    }

    @GetMapping
    public PageResponse<LikedTrackResponse>
            getLikedTracks(

                    @AuthenticationPrincipal
                    Jwt jwt,

                    @RequestParam(
                            defaultValue = "0"
                    )
                    int page,

                    @RequestParam(
                            defaultValue = "20"
                    )
                    int size) {

        UUID userId =
                getUserId(jwt);

        return likedTrackService
                .getLikedTracks(
                        userId,
                        page,
                        size
                );
    }

    private UUID getUserId(
            Jwt jwt) {

        return UUID.fromString(
                jwt.getSubject()
        );
    }
}