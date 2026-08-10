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

    private final TrackPlayHourlyStatsRepository
            trackPlayHourlyStatsRepository;

    public PlaybackAnalyticsService(
            ProcessedPlaybackEventRepository
                    processedPlaybackEventRepository,
            TrackPlayStatsRepository
                    trackPlayStatsRepository,
            TrackPlayHourlyStatsRepository
                    trackPlayHourlyStatsRepository) {

        this.processedPlaybackEventRepository =
                processedPlaybackEventRepository;

        this.trackPlayStatsRepository =
                trackPlayStatsRepository;

        this.trackPlayHourlyStatsRepository =
                trackPlayHourlyStatsRepository;
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
             * Kafka may redeliver an event.
             *
             * The processed event table ensures
             * both the all-time aggregate and
             * hourly aggregate are incremented
             * only once.
             */
            return;
        }

        /*
         * Existing all-time aggregate.
         */
        trackPlayStatsRepository
                .increment(
                        event
                );

        /*
         * New time-bucketed aggregate used
         * for trending calculations.
         */
        trackPlayHourlyStatsRepository
                .increment(
                        event
                );
    }
}