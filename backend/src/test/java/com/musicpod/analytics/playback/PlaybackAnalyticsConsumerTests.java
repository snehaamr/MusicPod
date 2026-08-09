package com.musicpod.analytics.playback;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.musicpod.messaging.event.PlaybackRecordedEvent;

@ExtendWith(MockitoExtension.class)
class PlaybackAnalyticsConsumerTests {

    @Mock
    private PlaybackAnalyticsService playbackAnalyticsService;

    @Mock
    private PlaybackRecordedEvent event;

    @Test
    void delegatesPlaybackEventToAnalyticsService() {

        PlaybackAnalyticsConsumer consumer =
                new PlaybackAnalyticsConsumer(
                        playbackAnalyticsService
                );

        consumer.consume(
                event
        );

        verify(
                playbackAnalyticsService
        ).process(
                event
        );
    }
}