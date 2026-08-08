package com.musicpod.library.playlist;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaylistTrackRepository
        extends JpaRepository<
                PlaylistTrack,
                PlaylistTrackId
        > {

    @Query("""
            SELECT COALESCE(
                MAX(playlistTrack.position),
                0
            )
            FROM PlaylistTrack playlistTrack
            WHERE playlistTrack.playlist.id = :playlistId
            """)
    int findMaxPosition(
            @Param("playlistId")
            UUID playlistId
    );

    @EntityGraph(
            attributePaths = {
                    "track",
                    "track.album"
            }
    )
    @Query(
            value = """
                    SELECT playlistTrack
                    FROM PlaylistTrack playlistTrack
                    WHERE playlistTrack.playlist.id = :playlistId
                    ORDER BY
                        playlistTrack.position ASC
                    """,
            countQuery = """
                    SELECT COUNT(playlistTrack)
                    FROM PlaylistTrack playlistTrack
                    WHERE playlistTrack.playlist.id = :playlistId
                    """
    )
    Page<PlaylistTrack> findPageByPlaylistId(
            @Param("playlistId")
            UUID playlistId,
            Pageable pageable
    );

    @Modifying
    @Query("""
            DELETE
            FROM PlaylistTrack playlistTrack
            WHERE playlistTrack.playlist.id = :playlistId
              AND playlistTrack.track.id = :trackId
            """)
    int deleteTrack(
            @Param("playlistId")
            UUID playlistId,

            @Param("trackId")
            UUID trackId
    );

    long countByPlaylist_Id(
            UUID playlistId
    );
}