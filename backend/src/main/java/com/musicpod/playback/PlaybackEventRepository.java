package com.musicpod.playback;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlaybackEventRepository
        extends JpaRepository<
                PlaybackEvent,
                UUID
        > {

    @EntityGraph(
            attributePaths = {
                    "track",
                    "track.album"
            }
    )
    @Query(
            value = """
                    SELECT playbackEvent
                    FROM PlaybackEvent playbackEvent
                    WHERE playbackEvent.user.id = :userId
                    ORDER BY
                        playbackEvent.playedAt DESC,
                        playbackEvent.id DESC
                    """,
            countQuery = """
                    SELECT COUNT(playbackEvent)
                    FROM PlaybackEvent playbackEvent
                    WHERE playbackEvent.user.id = :userId
                    """
    )
    Page<PlaybackEvent> findRecentByUserId(
            @Param("userId")
            UUID userId,
            Pageable pageable
    );

    long countByUser_Id(
            UUID userId
    );
}