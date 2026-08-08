package com.musicpod.playback;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.musicpod.common.api.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/me")
public class PlaybackController {

    private final PlaybackService playbackService;

    public PlaybackController(
            PlaybackService playbackService) {

        this.playbackService =
                playbackService;
    }

    @PostMapping("/playback-events")
    @ResponseStatus(HttpStatus.CREATED)
    public PlaybackEventResponse recordPlayback(
            @AuthenticationPrincipal
            Jwt jwt,

            @Valid
            @RequestBody
            RecordPlaybackRequest request) {

        return playbackService.recordPlayback(
                getUserId(jwt),
                request
        );
    }

    @GetMapping("/recently-played")
    public PageResponse<PlaybackEventResponse>
            getRecentlyPlayed(

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

        return playbackService
                .getRecentlyPlayed(
                        getUserId(jwt),
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