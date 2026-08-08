package com.musicpod.library.likedtrack;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.catalog.track.Track;
import com.musicpod.catalog.track.TrackRepository;
import com.musicpod.common.api.PageResponse;
import com.musicpod.common.exception.ResourceNotFoundException;
import com.musicpod.user.UserAccount;
import com.musicpod.user.UserRepository;

@Service
public class LikedTrackService {

    private static final int MAX_PAGE_SIZE = 100;

    private final LikedTrackRepository likedTrackRepository;
    private final UserRepository userRepository;
    private final TrackRepository trackRepository;

    public LikedTrackService(
            LikedTrackRepository likedTrackRepository,
            UserRepository userRepository,
            TrackRepository trackRepository) {

        this.likedTrackRepository =
                likedTrackRepository;

        this.userRepository =
                userRepository;

        this.trackRepository =
                trackRepository;
    }

    @Transactional
    public LikedTrackResponse like(
            UUID userId,
            UUID trackId) {

        UserAccount user =
                findUser(userId);

        Track track =
                findTrack(trackId);

        LikedTrackId likedTrackId =
                new LikedTrackId(
                        userId,
                        trackId
                );

        likedTrackRepository.insertIfAbsent(
                userId,
                trackId
        );

        LikedTrack likedTrack =
                likedTrackRepository
                        .findById(likedTrackId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Liked track was not created"
                                )
                        );

        /*
         * The row was inserted through a native query.
         *
         * We already loaded user and track above both for
         * validation and for predictable 404 responses.
         *
         * Accessing the associations here remains inside the
         * transaction.
         */
        likedTrack.getUser();
        track.getId();

        return LikedTrackResponse.from(
                likedTrack
        );
    }

    @Transactional
    public void unlike(
            UUID userId,
            UUID trackId) {

        findUser(userId);

        /*
         * This intentionally does not require the track
         * itself to exist.
         *
         * DELETE is idempotent:
         * after this call, the relationship does not exist.
         */
        likedTrackRepository.deleteForUser(
                userId,
                trackId
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<LikedTrackResponse>
            getLikedTracks(
                    UUID userId,
                    int page,
                    int size) {

        validatePagination(
                page,
                size
        );

        findUser(userId);

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        Page<LikedTrackResponse> likedTracks =
                likedTrackRepository
                        .findPageByUserId(
                                userId,
                                pageable
                        )
                        .map(
                                LikedTrackResponse::from
                        );

        return PageResponse.from(
                likedTracks
        );
    }

    private UserAccount findUser(
            UUID userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: "
                                        + userId
                        )
                );
    }

    private Track findTrack(
            UUID trackId) {

        return trackRepository
                .findById(trackId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Track not found: "
                                        + trackId
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

        if (size < 1
                || size > MAX_PAGE_SIZE) {

            throw new IllegalArgumentException(
                    "Size must be between 1 and "
                            + MAX_PAGE_SIZE
            );
        }
    }
}