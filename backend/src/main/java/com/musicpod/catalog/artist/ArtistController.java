package com.musicpod.catalog.artist;

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
@RequestMapping("/api/v1/artists")
public class ArtistController {

    private final ArtistService artistService;

    public ArtistController(ArtistService artistService) {
        this.artistService = artistService;
    }

    @PostMapping
    public ResponseEntity<ArtistResponse> create(
            @Valid @RequestBody CreateArtistRequest request,
            UriComponentsBuilder uriBuilder) {

        ArtistResponse artist =
                artistService.create(request);

        URI location = uriBuilder
                .path("/api/v1/artists/{id}")
                .buildAndExpand(artist.id())
                .toUri();

        return ResponseEntity
                .created(location)
                .body(artist);
    }

    @GetMapping("/{artistId}")
    public ArtistResponse getById(
            @PathVariable UUID artistId) {

        return artistService.getById(artistId);
    }

    @GetMapping
    public PageResponse<ArtistResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return artistService.getAll(page, size);
    }

    @PutMapping("/{artistId}")
    public ArtistResponse update(
            @PathVariable UUID artistId,
            @Valid @RequestBody UpdateArtistRequest request) {

        return artistService.update(
                artistId,
                request
        );
    }

    @DeleteMapping("/{artistId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID artistId) {

        artistService.delete(artistId);

        return ResponseEntity.noContent().build();
    }
}