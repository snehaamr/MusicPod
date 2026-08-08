package com.musicpod.library.likedtrack;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikedTrackRepository
        extends JpaRepository<
                LikedTrack,
                LikedTrackId
        > {

    @Modifying
    @Query(
            value = """
                    INSERT INTO liked_tracks (
                        user_id,
                        track_id,
                        liked_at
                    )
                    VALUES (
                        :userId,
                        :trackId,
                        CURRENT_TIMESTAMP
                    )
                    ON CONFLICT (
                        user_id,
                        track_id
                    )
                    DO NOTHING
                    """,
            nativeQuery = true
    )
    int insertIfAbsent(
            @Param("userId")
            UUID userId,

            @Param("trackId")
            UUID trackId
    );

    @Modifying
    @Query("""
            DELETE
            FROM LikedTrack likedTrack
            WHERE likedTrack.id.userId = :userId
              AND likedTrack.id.trackId = :trackId
            """)
    int deleteForUser(
            @Param("userId")
            UUID userId,

            @Param("trackId")
            UUID trackId
    );

    @EntityGraph(
            attributePaths = {
                    "track",
                    "track.album"
            }
    )
    @Query(
            value = """
                    SELECT likedTrack
                    FROM LikedTrack likedTrack
                    WHERE likedTrack.user.id = :userId
                    ORDER BY
                        likedTrack.likedAt DESC,
                        likedTrack.track.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(likedTrack)
                    FROM LikedTrack likedTrack
                    WHERE likedTrack.user.id = :userId
                    """
    )
    Page<LikedTrack> findPageByUserId(
            @Param("userId")
            UUID userId,
            Pageable pageable
    );

    long countByUser_Id(UUID userId);
}