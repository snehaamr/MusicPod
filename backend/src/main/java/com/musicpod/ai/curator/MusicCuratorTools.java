package com.musicpod.ai.curator;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.musicpod.catalog.track.TrackResponse;
import com.musicpod.catalog.track.TrackService;
import com.musicpod.search.track.HybridTrackSearchService;
import com.musicpod.search.track.TrackSearchResult;

@Component
public class MusicCuratorTools {

    private static final int MAX_SEARCH_RESULTS = 10;
    private static final int MAX_DURATION_TRACKS = 25;

    private final HybridTrackSearchService hybridTrackSearchService;
    private final TrackService trackService;

    public MusicCuratorTools(
            HybridTrackSearchService hybridTrackSearchService,
            TrackService trackService) {

        this.hybridTrackSearchService =
                hybridTrackSearchService;

        this.trackService =
                trackService;
    }

    @Tool(
            description = """
                    Search MusicPod's track catalog.

                    Use this before recommending music.

                    The result contains real tracks that exist in MusicPod.
                    Never invent tracks that are not returned by this tool.

                    When the user requests multiple specific artists,
                    search for each artist separately so tracks from every
                    requested artist can be considered.
                    """
    )
    public List<TrackSearchResult> searchTracks(

            @ToolParam(
                    description = """
                            Natural-language search query.
                            Examples:
                            'Kishore Kumar',
                            'romantic Arijit Singh songs',
                            'Queen rock'
                            """
            )
            String query,

            @ToolParam(
                    description = """
                            Maximum number of tracks to return.
                            Must be between 1 and 10.
                            """
            )
            int size) {

        int normalizedSize =
                Math.max(
                        1,
                        Math.min(
                                size,
                                MAX_SEARCH_RESULTS
                        )
                );

        return hybridTrackSearchService.search(
                query,
                normalizedSize
        );
    }

    @Tool(
            description = """
                    Get complete details for one MusicPod track.

                    Use this when additional information about a track
                    is needed. The trackId must come from MusicPod search
                    results or another MusicPod tool.
                    """
    )
    public TrackResponse getTrack(

            @ToolParam(
                    description = "MusicPod track UUID"
            )
            String trackId) {

        UUID id =
                UUID.fromString(
                        trackId
                );

        return trackService.getById(
                id
        );
    }

    @Tool(
            description = """
                    Calculate the exact combined duration of MusicPod tracks.

                    Use this when the user requests a playlist with a
                    duration constraint such as 10 minutes, 30 minutes,
                    or about one hour.

                    All track IDs must come from MusicPod tools.
                    """
    )
    public PlaylistDurationResult calculatePlaylistDuration(

            @ToolParam(
                    description = """
                            MusicPod track UUIDs whose total duration
                            should be calculated.
                            """
            )
            List<String> trackIds) {

        if (trackIds == null
                || trackIds.isEmpty()) {

            return new PlaylistDurationResult(
                    0,
                    0,
                    "0:00"
            );
        }

        if (trackIds.size()
                > MAX_DURATION_TRACKS) {

            throw new IllegalArgumentException(
                    "Cannot calculate more than "
                            + MAX_DURATION_TRACKS
                            + " tracks at once"
            );
        }

        long totalDurationMs = 0;

        for (String trackId : trackIds) {

            TrackResponse track =
                    trackService.getById(
                            UUID.fromString(
                                    trackId
                            )
                    );

            totalDurationMs +=
                    track.durationMs();
        }

        return new PlaylistDurationResult(
                trackIds.size(),
                totalDurationMs,
                formatDuration(
                        totalDurationMs
                )
        );
    }

    private String formatDuration(
            long totalDurationMs) {

        long totalSeconds =
                totalDurationMs / 1000;

        long minutes =
                totalSeconds / 60;

        long seconds =
                totalSeconds % 60;

        return "%d:%02d".formatted(
                minutes,
                seconds
        );
    }
}