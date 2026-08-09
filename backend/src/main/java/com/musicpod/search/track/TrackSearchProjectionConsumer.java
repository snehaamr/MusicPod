package com.musicpod.search.track;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.musicpod.messaging.event.TrackSearchDeletedEvent;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;
import com.musicpod.messaging.kafka.KafkaTopics;
import org.apache.kafka.clients.consumer.ConsumerRecord;

@Component
@ConditionalOnProperty(
        name = "app.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class TrackSearchProjectionConsumer {

    private final TrackSearchProjectionService
            trackSearchProjectionService;

    public TrackSearchProjectionConsumer(
            TrackSearchProjectionService
                    trackSearchProjectionService) {

        this.trackSearchProjectionService =
                trackSearchProjectionService;
    }

    @KafkaListener(
            topics = KafkaTopics.TRACK_SEARCH_CHANGED,
            groupId = "musicpod-track-search-projection-v1"
    )
    public void consume(
            ConsumerRecord<String, Object> record) {

        Object event = record.value();

        if (event instanceof TrackSearchUpsertedEvent upsertedEvent) {

        	trackSearchProjectionService.upsert(
                    upsertedEvent
            );

            return;
        }

        if (event instanceof TrackSearchDeletedEvent deletedEvent) {

        	trackSearchProjectionService.delete(
                    deletedEvent
            );

            return;
        }

        throw new IllegalArgumentException(
                "Unsupported track search event: "
                        + (event == null
                            ? "null"
                            : event.getClass().getName())
        );
    }
}