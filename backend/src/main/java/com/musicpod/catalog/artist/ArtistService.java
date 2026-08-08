package com.musicpod.catalog.artist;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.common.api.PageResponse;
import com.musicpod.common.exception.ResourceNotFoundException;

@Service
public class ArtistService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ArtistRepository artistRepository;

    public ArtistService(ArtistRepository artistRepository) {
        this.artistRepository = artistRepository;
    }

    @Transactional
    public ArtistResponse create(
            CreateArtistRequest request) {

        Artist artist = new Artist(
                request.name(),
                request.bio(),
                request.imageUrl()
        );

        Artist savedArtist =
                artistRepository.save(artist);

        return ArtistResponse.from(savedArtist);
    }

    @Transactional(readOnly = true)
    public ArtistResponse getById(UUID artistId) {

        Artist artist = findArtist(artistId);

        return ArtistResponse.from(artist);
    }

    @Transactional(readOnly = true)
    public PageResponse<ArtistResponse> getAll(
            int page,
            int size) {

        validatePagination(page, size);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("name"),
                        Sort.Order.asc("id")
                )
        );

        Page<ArtistResponse> artists =
                artistRepository
                        .findAll(pageable)
                        .map(ArtistResponse::from);

        return PageResponse.from(artists);
    }

    @Transactional
    public ArtistResponse update(
            UUID artistId,
            UpdateArtistRequest request) {

        Artist artist = findArtist(artistId);

        artist.update(
                request.name(),
                request.bio(),
                request.imageUrl()
        );

        return ArtistResponse.from(artist);
    }

    @Transactional
    public void delete(UUID artistId) {

        Artist artist = findArtist(artistId);

        artistRepository.delete(artist);
    }

    private Artist findArtist(UUID artistId) {

        return artistRepository
                .findById(artistId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Artist not found: " + artistId
                        )
                );
    }

    private void validatePagination(
            int page,
            int size) {

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }
    }
}