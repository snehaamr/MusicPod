package com.musicpod.catalog.track;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api/v1")
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    @PostMapping("/albums/{albumId}/tracks")
    public ResponseEntity<TrackResponse> create(
            @PathVariable UUID albumId,
            @Valid @RequestBody CreateTrackRequest request,
            UriComponentsBuilder uriBuilder) {

        TrackResponse track =
                trackService.create(
                        albumId,
                        request
                );

        URI location = uriBuilder
                .path("/api/v1/tracks/{id}")
                .buildAndExpand(track.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(track);
    }

    @GetMapping("/tracks/{trackId}")
    public TrackResponse getById(
            @PathVariable UUID trackId) {

        return trackService.getById(trackId);
    }

    @GetMapping("/albums/{albumId}/tracks")
    public PageResponse<TrackResponse> getByAlbum(
            @PathVariable UUID albumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return trackService.getByAlbum(
                albumId,
                page,
                size
        );
    }

    @PutMapping("/tracks/{trackId}")
    public TrackResponse update(
            @PathVariable UUID trackId,
            @Valid @RequestBody UpdateTrackRequest request) {

        return trackService.update(
                trackId,
                request
        );
    }

    @DeleteMapping("/tracks/{trackId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID trackId) {

        trackService.delete(trackId);

        return ResponseEntity
                .noContent()
                .build();
    }
}