package com.musicpod.catalog.track;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
    
    @EntityGraph(
            attributePaths = {
                    "album",
                    "album.artist"
            }
    )
    @Query("""
            SELECT track
            FROM Track track
            WHERE track.id IN :trackIds
            """)
    List<Track> findAllByIdsWithAlbumAndArtist(
            @Param("trackIds")
            Collection<UUID> trackIds
    );
}