package com.musicpod.catalog.album;

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
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @PostMapping("/artists/{artistId}/albums")
    public ResponseEntity<AlbumResponse> create(
            @PathVariable UUID artistId,
            @Valid @RequestBody CreateAlbumRequest request,
            UriComponentsBuilder uriBuilder) {

        AlbumResponse album =
                albumService.create(
                        artistId,
                        request
                );

        URI location = uriBuilder
                .path("/api/v1/albums/{id}")
                .buildAndExpand(album.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(album);
    }

    @GetMapping("/albums/{albumId}")
    public AlbumResponse getById(
            @PathVariable UUID albumId) {

        return albumService.getById(albumId);
    }

    @GetMapping("/artists/{artistId}/albums")
    public PageResponse<AlbumResponse> getByArtist(
            @PathVariable UUID artistId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return albumService.getByArtist(
                artistId,
                page,
                size
        );
    }

    @PutMapping("/albums/{albumId}")
    public AlbumResponse update(
            @PathVariable UUID albumId,
            @Valid @RequestBody UpdateAlbumRequest request) {

        return albumService.update(
                albumId,
                request
        );
    }

    @DeleteMapping("/albums/{albumId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID albumId) {

        albumService.delete(albumId);

        return ResponseEntity
                .noContent()
                .build();
    }
}