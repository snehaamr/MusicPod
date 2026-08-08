package com.musicpod.analytics.playback;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.messaging.event.PlaybackRecordedEvent;

@Service
public class PlaybackAnalyticsService {

    private final ProcessedPlaybackEventRepository
            processedPlaybackEventRepository;

    private final TrackPlayStatsRepository
            trackPlayStatsRepository;

    public PlaybackAnalyticsService(
            ProcessedPlaybackEventRepository processedPlaybackEventRepository,
            TrackPlayStatsRepository trackPlayStatsRepository) {

        this.processedPlaybackEventRepository =
                processedPlaybackEventRepository;

        this.trackPlayStatsRepository =
                trackPlayStatsRepository;
    }

    @Transactional
    public void process(
            PlaybackRecordedEvent event) {

        boolean firstProcessing =
                processedPlaybackEventRepository
                        .markProcessedIfAbsent(
                                event.eventId()
                        );

        if (!firstProcessing) {

            /*
             * Kafka delivered an event that we have
             * already processed.
             *
             * Idempotently ignore it.
             */
            return;
        }

        trackPlayStatsRepository
                .increment(event);
    }
}