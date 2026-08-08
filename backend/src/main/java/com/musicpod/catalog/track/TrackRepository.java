package com.musicpod.catalog.track;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackRepository
        extends JpaRepository<Track, UUID> {

    Page<Track> findByAlbum_Id(
            UUID albumId,
            Pageable pageable
    );

    boolean existsByAlbum_IdAndTrackNumber(
            UUID albumId,
            int trackNumber
    );

    boolean existsByAlbum_IdAndTrackNumberAndIdNot(
            UUID albumId,
            int trackNumber,
            UUID trackId
    );
}