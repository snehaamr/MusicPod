package com.musicpod.catalog.album;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.catalog.artist.Artist;
import com.musicpod.catalog.artist.ArtistRepository;
import com.musicpod.common.api.PageResponse;
import com.musicpod.common.exception.ResourceNotFoundException;

@Service
public class AlbumService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AlbumRepository albumRepository;
    private final ArtistRepository artistRepository;

    public AlbumService(
            AlbumRepository albumRepository,
            ArtistRepository artistRepository) {

        this.albumRepository = albumRepository;
        this.artistRepository = artistRepository;
    }

    @Transactional
    public AlbumResponse create(
            UUID artistId,
            CreateAlbumRequest request) {

        Artist artist = findArtist(artistId);

        Album album = new Album(
                artist,
                request.title(),
                request.releaseDate(),
                request.coverImageUrl()
        );

        Album savedAlbum =
                albumRepository.save(album);

        return AlbumResponse.from(savedAlbum);
    }

    @Transactional(readOnly = true)
    public AlbumResponse getById(UUID albumId) {

        Album album = findAlbum(albumId);

        return AlbumResponse.from(album);
    }

    @Transactional(readOnly = true)
    public PageResponse<AlbumResponse> getByArtist(
            UUID artistId,
            int page,
            int size) {

        validatePagination(page, size);

        // Important:
        // distinguish "artist has zero albums"
        // from "artist does not exist".
        findArtist(artistId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("title"),
                        Sort.Order.asc("id")
                )
        );

        Page<AlbumResponse> albums =
                albumRepository
                        .findByArtist_Id(
                                artistId,
                                pageable
                        )
                        .map(AlbumResponse::from);

        return PageResponse.from(albums);
    }

    @Transactional
    public AlbumResponse update(
            UUID albumId,
            UpdateAlbumRequest request) {

        Album album = findAlbum(albumId);

        album.update(
                request.title(),
                request.releaseDate(),
                request.coverImageUrl()
        );

        return AlbumResponse.from(album);
    }

    @Transactional
    public void delete(UUID albumId) {

        Album album = findAlbum(albumId);

        albumRepository.delete(album);
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

    private Album findAlbum(UUID albumId) {

        return albumRepository
                .findById(albumId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Album not found: " + albumId
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