package com.musicpod.messaging.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.musicpod.messaging.event.PlaybackRecordedEvent;

@Component
@ConditionalOnProperty(
        name = "app.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PlaybackKafkaPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    PlaybackKafkaPublisher.class
            );

    private final KafkaTemplate<Object, Object>
            kafkaTemplate;

    public PlaybackKafkaPublisher(
            KafkaTemplate<Object, Object> kafkaTemplate) {

        this.kafkaTemplate =
                kafkaTemplate;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void publish(
            PlaybackRecordedEvent event) {

        String key =
                event.trackId()
                        .toString();

        kafkaTemplate
                .send(
                        KafkaTopics.PLAYBACK_RECORDED,
                        key,
                        event
                )
                .whenComplete(
                        (result, exception) -> {

                            if (exception != null) {

                                log.error(
                                        "Failed to publish playback event {}",
                                        event.eventId(),
                                        exception
                                );

                                return;
                            }

                            log.info(
                                    "Published playback event {} to partition {} offset {}",
                                    event.eventId(),
                                    result
                                            .getRecordMetadata()
                                            .partition(),
                                    result
                                            .getRecordMetadata()
                                            .offset()
                            );
                        }
                );
    }
}