package com.musicpod.mcp;

import java.util.List;
import java.util.Map;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import com.musicpod.common.api.PageResponse;
import com.musicpod.library.likedtrack.LikedTrackResponse;
import com.musicpod.library.likedtrack.LikedTrackService;
import com.musicpod.playback.PlaybackEventResponse;
import com.musicpod.playback.PlaybackService;
import com.musicpod.recommendation.RecommendationResponse;
import com.musicpod.recommendation.RecommendationService;

@Component
public class MusicPodMcpPersonalizedTools {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 20;

    private final LikedTrackService
            likedTrackService;

    private final PlaybackService
            playbackService;

    private final McpAuditService
            mcpAuditService;

    private final RecommendationService
            recommendationService;

    public MusicPodMcpPersonalizedTools(
            LikedTrackService likedTrackService,
            PlaybackService playbackService,
            McpAuditService mcpAuditService,
            RecommendationService recommendationService) {

        this.likedTrackService =
                likedTrackService;

        this.playbackService =
                playbackService;

        this.mcpAuditService =
                mcpAuditService;

        this.recommendationService =
                recommendationService;
    }

    @McpTool(
            name = "musicpod_get_liked_tracks",
            title = "Get My Liked Tracks",
            description =
                    "Returns tracks liked by the currently "
                    + "authenticated MusicPod user. "
                    + "User identity is derived from the "
                    + "authenticated JWT.",
            annotations =
                    @McpTool.McpAnnotations(
                            readOnlyHint = true,
                            destructiveHint = false,
                            idempotentHint = true,
                            openWorldHint = false
                    )
    )
    public PageResponse<LikedTrackResponse>
            getLikedTracks(

            @McpToolParam(
                    description =
                            "Zero-based page number. "
                            + "Defaults to 0.",
                    required = false
            )
            Integer page,

            @McpToolParam(
                    description =
                            "Number of liked tracks to return. "
                            + "Must be between 1 and 20. "
                            + "Defaults to 10.",
                    required = false
            )
            Integer size) {

        int requestedPage =
                page == null
                        ? DEFAULT_PAGE
                        : page;

        int requestedSize =
                size == null
                        ? DEFAULT_SIZE
                        : size;

        return mcpAuditService.execute(
                "musicpod_get_liked_tracks",
                false,
                McpAuditService.RISK_READ_ONLY,
                Map.of(
                        "page",
                        requestedPage,
                        "size",
                        requestedSize
                ),
                userId -> {

                    int effectivePage =
                            normalizePage(page);

                    int effectiveSize =
                            normalizeSize(size);

                    return likedTrackService
                            .getLikedTracks(
                                    userId,
                                    effectivePage,
                                    effectiveSize
                            );
                }
        );
    }

    @McpTool(
            name = "musicpod_get_recently_played",
            title = "Get My Recently Played",
            description =
                    "Returns recent playback events for the "
                    + "currently authenticated MusicPod user. "
                    + "User identity is derived from the "
                    + "authenticated JWT.",
            annotations =
                    @McpTool.McpAnnotations(
                            readOnlyHint = true,
                            destructiveHint = false,
                            idempotentHint = true,
                            openWorldHint = false
                    )
    )
    public PageResponse<PlaybackEventResponse>
            getRecentlyPlayed(

            @McpToolParam(
                    description =
                            "Zero-based page number. "
                            + "Defaults to 0.",
                    required = false
            )
            Integer page,

            @McpToolParam(
                    description =
                            "Number of playback events to return. "
                            + "Must be between 1 and 20. "
                            + "Defaults to 10.",
                    required = false
            )
            Integer size) {

        int requestedPage =
                page == null
                        ? DEFAULT_PAGE
                        : page;

        int requestedSize =
                size == null
                        ? DEFAULT_SIZE
                        : size;

        return mcpAuditService.execute(
                "musicpod_get_recently_played",
                false,
                McpAuditService.RISK_READ_ONLY,
                Map.of(
                        "page",
                        requestedPage,
                        "size",
                        requestedSize
                ),
                userId -> {

                    int effectivePage =
                            normalizePage(page);

                    int effectiveSize =
                            normalizeSize(size);

                    return playbackService
                            .getRecentlyPlayed(
                                    userId,
                                    effectivePage,
                                    effectiveSize
                            );
                }
        );
    }

    @McpTool(
            name = "musicpod_get_recommendations",
            title = "Get My Recommendations",
            description =
                    "Returns personalized track recommendations "
                    + "for the currently authenticated MusicPod user. "
                    + "Recommendations are derived from listening "
                    + "history and liked tracks. "
                    + "User identity is derived from the "
                    + "authenticated JWT.",
            annotations =
                    @McpTool.McpAnnotations(
                            readOnlyHint = true,
                            destructiveHint = false,
                            idempotentHint = true,
                            openWorldHint = false
                    )
    )
    public List<RecommendationResponse>
            getRecommendations(

            @McpToolParam(
                    description =
                            "Maximum number of recommendations "
                            + "to return. Must be between 1 and 20. "
                            + "Defaults to 10.",
                    required = false
            )
            Integer size) {

        int requestedSize =
                size == null
                        ? DEFAULT_SIZE
                        : size;

        return mcpAuditService.execute(
                "musicpod_get_recommendations",
                false,
                McpAuditService.RISK_READ_ONLY,
                Map.of(
                        "size",
                        requestedSize
                ),
                userId -> {

                    int effectiveSize =
                            normalizeSize(size);

                    return recommendationService
                            .getRecommendations(
                                    userId,
                                    effectiveSize
                            );
                }
        );
    }

    private int normalizePage(
            Integer page) {

        if (page == null) {
            return DEFAULT_PAGE;
        }

        if (page < 0) {

            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0"
            );
        }

        return page;
    }

    private int normalizeSize(
            Integer size) {

        if (size == null) {
            return DEFAULT_SIZE;
        }

        if (size < 1
                || size > MAX_SIZE) {

            throw new IllegalArgumentException(
                    "Size must be between 1 and "
                            + MAX_SIZE
            );
        }

        return size;
    }
}