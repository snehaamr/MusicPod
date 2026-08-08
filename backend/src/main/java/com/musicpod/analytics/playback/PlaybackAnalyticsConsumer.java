package com.musicpod.analytics.playback;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.musicpod.messaging.event.PlaybackRecordedEvent;
import com.musicpod.messaging.kafka.KafkaTopics;

@Component
@ConditionalOnProperty(
        name = "app.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PlaybackAnalyticsConsumer {

    private final PlaybackAnalyticsService
            playbackAnalyticsService;

    public PlaybackAnalyticsConsumer(
            PlaybackAnalyticsService playbackAnalyticsService) {

        this.playbackAnalyticsService =
                playbackAnalyticsService;
    }

    @KafkaListener(
            topics = KafkaTopics.PLAYBACK_RECORDED,
            groupId = "musicpod-playback-analytics-v1"
    )
    public void consume(
            PlaybackRecordedEvent event) {

        playbackAnalyticsService
                .process(event);
    }
}