package com.musicpod.catalog.track;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import com.musicpod.messaging.event.TrackSearchDeletedEvent;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;
import com.musicpod.catalog.album.Album;
import com.musicpod.catalog.album.AlbumRepository;
import com.musicpod.common.api.PageResponse;
import com.musicpod.common.exception.ResourceConflictException;
import com.musicpod.common.exception.ResourceNotFoundException;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import com.musicpod.config.CacheNames;

@Service
public class TrackService {

    private static final int MAX_PAGE_SIZE = 100;

    private final TrackRepository trackRepository;
    private final AlbumRepository albumRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    public TrackService(
            TrackRepository trackRepository,
            AlbumRepository albumRepository,
            ApplicationEventPublisher applicationEventPublisher) {

        this.trackRepository = trackRepository;
        this.albumRepository = albumRepository;
        this.applicationEventPublisher =
                applicationEventPublisher;
    }

    @Transactional
    public TrackResponse create(
            UUID albumId,
            CreateTrackRequest request) {

        Album album = findAlbum(albumId);

        ensureTrackNumberAvailable(
                albumId,
                request.trackNumber(),
                null
        );

        Track track = new Track(
                album,
                request.title(),
                request.trackNumber(),
                request.durationMs(),
                request.explicit()
        );

        try {

            Track savedTrack =
                    trackRepository.saveAndFlush(track);
            
            applicationEventPublisher.publishEvent(
                    TrackSearchUpsertedEvent.from(
                            savedTrack
                    )
            );

            return TrackResponse.from(savedTrack);

        } catch (DataIntegrityViolationException exception) {

            throw duplicateTrackNumberException(
                    albumId,
                    request.trackNumber()
            );
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = CacheNames.TRACKS,
            key = "#trackId"
    )
    public TrackResponse getById(
            UUID trackId) {

        Track track =
                trackRepository
                        .findById(trackId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Track not found"
                                        )
                        );

        return TrackResponse.from(track);
    }

    @Transactional(readOnly = true)
    public PageResponse<TrackResponse> getByAlbum(
            UUID albumId,
            int page,
            int size) {

        validatePagination(page, size);

        // Distinguish:
        // "album exists but has no tracks"
        // from:
        // "album does not exist".
        findAlbum(albumId);

        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("trackNumber"),
                        Sort.Order.asc("id")
                )
        );

        Page<TrackResponse> tracks =
                trackRepository
                        .findByAlbum_Id(
                                albumId,
                                pageable
                        )
                        .map(TrackResponse::from);

        return PageResponse.from(tracks);
    }

    @Transactional
    @CacheEvict(
            cacheNames = CacheNames.TRACKS,
            key = "#trackId"
    )
    public TrackResponse update(
            UUID trackId,
            UpdateTrackRequest request) {

        Track track = findTrack(trackId);

        UUID albumId =
                track.getAlbum().getId();

        ensureTrackNumberAvailable(
                albumId,
                request.trackNumber(),
                trackId
        );

        track.update(
                request.title(),
                request.trackNumber(),
                request.durationMs(),
                request.explicit()
        );

        try {

            trackRepository.flush();
            applicationEventPublisher.publishEvent(
                    TrackSearchUpsertedEvent.from(
                            track
                    )
            );

            return TrackResponse.from(track);

        } catch (DataIntegrityViolationException exception) {

            throw duplicateTrackNumberException(
                    albumId,
                    request.trackNumber()
            );
        }
    }

    @Transactional
    @CacheEvict(
            cacheNames = CacheNames.TRACKS,
            key = "#trackId"
    )
    public void delete(UUID trackId) {

        Track track = findTrack(trackId);
        
        UUID deletedTrackId =
                track.getId();

        trackRepository.delete(track);

        applicationEventPublisher.publishEvent(
                TrackSearchDeletedEvent.from(
                        deletedTrackId
                )
        );

        trackRepository.delete(track);
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

    private Track findTrack(UUID trackId) {

        return trackRepository
                .findById(trackId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Track not found: " + trackId
                        )
                );
    }
    

    private void ensureTrackNumberAvailable(
            UUID albumId,
            int trackNumber,
            UUID excludedTrackId) {

        boolean exists;

        if (excludedTrackId == null) {

            exists =
                    trackRepository
                            .existsByAlbum_IdAndTrackNumber(
                                    albumId,
                                    trackNumber
                            );

        } else {

            exists =
                    trackRepository
                            .existsByAlbum_IdAndTrackNumberAndIdNot(
                                    albumId,
                                    trackNumber,
                                    excludedTrackId
                            );
        }

        if (exists) {

            throw duplicateTrackNumberException(
                    albumId,
                    trackNumber
            );
        }
    }

    private ResourceConflictException
            duplicateTrackNumberException(
                    UUID albumId,
                    int trackNumber) {

        return new ResourceConflictException(
                "Track number "
                        + trackNumber
                        + " already exists for album "
                        + albumId
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