package com.musicpod.playback;

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
public class PlaybackService {

    private static final int MAX_PAGE_SIZE = 100;

    private final PlaybackEventRepository
            playbackEventRepository;

    private final UserRepository
            userRepository;

    private final TrackRepository
            trackRepository;

    public PlaybackService(
            PlaybackEventRepository playbackEventRepository,
            UserRepository userRepository,
            TrackRepository trackRepository) {

        this.playbackEventRepository =
                playbackEventRepository;

        this.userRepository =
                userRepository;

        this.trackRepository =
                trackRepository;
    }

    @Transactional
    public PlaybackEventResponse recordPlayback(
            UUID userId,
            RecordPlaybackRequest request) {

        UserAccount user =
                findUser(userId);

        Track track =
                findTrack(
                        request.trackId()
                );

        validatePlayedDuration(
                request.playedMs(),
                track
        );

        PlaybackEvent event =
                new PlaybackEvent(
                        user,
                        track,
                        request.playedMs()
                );

        PlaybackEvent savedEvent =
                playbackEventRepository
                        .save(event);

        return PlaybackEventResponse.from(
                savedEvent
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<PlaybackEventResponse>
            getRecentlyPlayed(
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

        Page<PlaybackEventResponse> events =
                playbackEventRepository
                        .findRecentByUserId(
                                userId,
                                pageable
                        )
                        .map(
                                PlaybackEventResponse::from
                        );

        return PageResponse.from(
                events
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

    private void validatePlayedDuration(
            int playedMs,
            Track track) {

        if (playedMs > track.getDurationMs()) {

            throw new IllegalArgumentException(
                    "Played milliseconds must not exceed track duration"
            );
        }
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