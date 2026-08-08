package com.musicpod.library.playlist;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.musicpod.common.api.PageResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/me/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(
            PlaylistService playlistService) {

        this.playlistService =
                playlistService;
    }

    @PostMapping
    public ResponseEntity<PlaylistResponse> create(
            @AuthenticationPrincipal
            Jwt jwt,

            @Valid
            @RequestBody
            CreatePlaylistRequest request,

            UriComponentsBuilder uriBuilder) {

        PlaylistResponse playlist =
                playlistService.create(
                        getUserId(jwt),
                        request
                );

        URI location =
                uriBuilder
                        .path(
                                "/api/v1/me/playlists/{id}"
                        )
                        .buildAndExpand(
                                playlist.id()
                        )
                        .toUri();

        return ResponseEntity
                .created(location)
                .body(playlist);
    }

    @GetMapping
    public PageResponse<PlaylistResponse> getAll(
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

        return playlistService.getAll(
                getUserId(jwt),
                page,
                size
        );
    }

    @GetMapping("/{playlistId}")
    public PlaylistResponse getById(
            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID playlistId) {

        return playlistService.getById(
                getUserId(jwt),
                playlistId
        );
    }

    @PutMapping("/{playlistId}")
    public PlaylistResponse update(
            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID playlistId,

            @Valid
            @RequestBody
            UpdatePlaylistRequest request) {

        return playlistService.update(
                getUserId(jwt),
                playlistId,
                request
        );
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID playlistId) {

        playlistService.delete(
                getUserId(jwt),
                playlistId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    @PutMapping(
            "/{playlistId}/tracks/{trackId}"
    )
    public PlaylistTrackResponse addTrack(
            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID playlistId,

            @PathVariable
            UUID trackId) {

        return playlistService.addTrack(
                getUserId(jwt),
                playlistId,
                trackId
        );
    }

    @GetMapping(
            "/{playlistId}/tracks"
    )
    public PageResponse<PlaylistTrackResponse>
            getTracks(

                    @AuthenticationPrincipal
                    Jwt jwt,

                    @PathVariable
                    UUID playlistId,

                    @RequestParam(
                            defaultValue = "0"
                    )
                    int page,

                    @RequestParam(
                            defaultValue = "50"
                    )
                    int size) {

        return playlistService.getTracks(
                getUserId(jwt),
                playlistId,
                page,
                size
        );
    }

    @DeleteMapping(
            "/{playlistId}/tracks/{trackId}"
    )
    public ResponseEntity<Void> removeTrack(
            @AuthenticationPrincipal
            Jwt jwt,

            @PathVariable
            UUID playlistId,

            @PathVariable
            UUID trackId) {

        playlistService.removeTrack(
                getUserId(jwt),
                playlistId,
                trackId
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    private UUID getUserId(
            Jwt jwt) {

        return UUID.fromString(
                jwt.getSubject()
        );
    }
}