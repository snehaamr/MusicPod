package com.musicpod.ai.curator;

import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.musicpod.auth.CurrentUserProvider;
import com.musicpod.common.api.PageResponse;
import com.musicpod.library.likedtrack.LikedTrackResponse;
import com.musicpod.library.likedtrack.LikedTrackService;
import com.musicpod.playback.PlaybackEventResponse;
import com.musicpod.playback.PlaybackService;

@Component
public class PersonalizedCuratorTools {

    private static final int MAX_RESULTS = 20;

    private final CurrentUserProvider currentUserProvider;
    private final LikedTrackService likedTrackService;
    private final PlaybackService playbackService;

    public PersonalizedCuratorTools(
            CurrentUserProvider currentUserProvider,
            LikedTrackService likedTrackService,
            PlaybackService playbackService) {

        this.currentUserProvider =
                currentUserProvider;

        this.likedTrackService =
                likedTrackService;

        this.playbackService =
                playbackService;
    }

    @Tool(description = """
            Get tracks liked by the currently authenticated MusicPod user.

            Use this when the user asks for recommendations based on
            their preferences, favorites, likes, or taste.

            The authenticated user is determined by MusicPod from the
            JWT. Never ask the user for a user ID.
            """)
    public PageResponse<LikedTrackResponse> getLikedTracks(

            @ToolParam(
                    description =
                            "Maximum number of liked tracks to return, from 1 to 20"
            )
            int size) {

        UUID userId =
                currentUserProvider.userId();

        int normalizedSize =
                Math.max(
                        1,
                        Math.min(
                                size,
                                MAX_RESULTS
                        )
                );

        return likedTrackService
                .getLikedTracks(
                        userId,
                        0,
                        normalizedSize
                );
    }

    @Tool(description = """
            Get the most recently played tracks for the currently
            authenticated MusicPod user.

            Use this when the user's listening history is relevant
            to a recommendation or playlist request.

            Results are returned newest first by the existing
            MusicPod playback service.

            The authenticated user is determined by MusicPod from the
            JWT. Never ask the user for a user ID.
            """)
    public PageResponse<PlaybackEventResponse> getRecentlyPlayed(

            @ToolParam(
                    description =
                            "Maximum number of recent playback events to return, from 1 to 20"
            )
            int size) {

        UUID userId =
                currentUserProvider.userId();

        int normalizedSize =
                Math.max(
                        1,
                        Math.min(
                                size,
                                MAX_RESULTS
                        )
                );

        return playbackService
                .getRecentlyPlayed(
                        userId,
                        0,
                        normalizedSize
                );
    }
}