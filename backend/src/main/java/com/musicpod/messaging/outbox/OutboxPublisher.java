package com.musicpod.messaging.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.musicpod.messaging.event.PlaybackRecordedEvent;
import com.musicpod.messaging.event.TrackSearchDeletedEvent;
import com.musicpod.messaging.event.TrackSearchUpsertedEvent;

import tools.jackson.databind.json.JsonMapper;
import io.micrometer.core.instrument.Timer;

@Component
@ConditionalOnProperty(
        name = "app.kafka.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OutboxPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(
                    OutboxPublisher.class
            );

    private static final int MAX_ERROR_LENGTH =
            2000;

    private final OutboxEventRepository
            outboxEventRepository;

    private final KafkaTemplate<Object, Object>
            kafkaTemplate;

    private final JsonMapper jsonMapper;

    private final int batchSize;

    private final int maxAttempts;

    private final long sendTimeoutSeconds;
    
    private final OutboxPublishMetrics outboxPublishMetrics;

    public OutboxPublisher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<Object, Object> kafkaTemplate,
            JsonMapper jsonMapper,
            OutboxPublishMetrics outboxPublishMetrics,
            @Value("${app.outbox.batch-size:25}")
            int batchSize,
            @Value("${app.outbox.max-attempts:20}")
            int maxAttempts,
            @Value("${app.outbox.send-timeout-seconds:10}")
            long sendTimeoutSeconds) {

        this.outboxEventRepository =
                outboxEventRepository;

        this.kafkaTemplate =
                kafkaTemplate;

        this.jsonMapper =
                jsonMapper;

        this.outboxPublishMetrics =
                outboxPublishMetrics;

        this.batchSize =
                batchSize;

        this.maxAttempts =
                maxAttempts;

        this.sendTimeoutSeconds =
                sendTimeoutSeconds;
    }

    @Scheduled(
            fixedDelayString =
                    "${app.outbox.poll-interval-ms:1000}",
            initialDelayString =
                    "${app.outbox.initial-delay-ms:1000}"
    )
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxEventRepository.claimBatch(
                        batchSize
                );

        for (OutboxEvent event : events) {

            if (!publish(event)) {
                return;
            }
        }
    }

    private boolean publish(
            OutboxEvent outboxEvent) {

        Timer.Sample timer =
                outboxPublishMetrics
                        .startTimer();

        try {

            Object event =
                    deserialize(
                            outboxEvent
                    );

            kafkaTemplate
                    .send(
                            outboxEvent.topic(),
                            outboxEvent.messageKey(),
                            event
                    )
                    .get(
                            sendTimeoutSeconds,
                            TimeUnit.SECONDS
                    );

            outboxEventRepository
                    .markPublished(
                            outboxEvent.id()
                    );

            outboxPublishMetrics
                    .recordSuccess(
                            timer
                    );

            log.info(
                    "Published outbox event {} type={} attempt={}",
                    outboxEvent.id(),
                    outboxEvent.eventType(),
                    outboxEvent.attempts()
            );

            return true;

        } catch (InterruptedException exception) {

            /*
             * Restore the interrupt flag because
             * Future.get() clears it when throwing
             * InterruptedException.
             */
            Thread.currentThread()
                    .interrupt();

            outboxPublishMetrics
                    .recordFailure(
                            timer
                    );

            handleFailure(
                    outboxEvent,
                    exception
            );

            return false;

        } catch (Exception exception) {

            outboxPublishMetrics
                    .recordFailure(
                            timer
                    );

            handleFailure(
                    outboxEvent,
                    exception
            );

            return true;
        }
    }

    private Object deserialize(
            OutboxEvent outboxEvent) {

        return switch (
                outboxEvent.eventType()
        ) {

            case PlaybackRecordedEvent.EVENT_TYPE ->
                    jsonMapper.readValue(
                            outboxEvent.payload(),
                            PlaybackRecordedEvent.class
                    );

            case TrackSearchUpsertedEvent.EVENT_TYPE ->
                    jsonMapper.readValue(
                            outboxEvent.payload(),
                            TrackSearchUpsertedEvent.class
                    );

            case TrackSearchDeletedEvent.EVENT_TYPE ->
                    jsonMapper.readValue(
                            outboxEvent.payload(),
                            TrackSearchDeletedEvent.class
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unsupported outbox event type: "
                                    + outboxEvent.eventType()
                    );
        };
    }

    private void handleFailure(
            OutboxEvent outboxEvent,
            Exception exception) {

        Throwable rootCause =
                rootCause(exception);

        String error =
                truncate(
                        rootCause.getClass()
                                .getSimpleName()
                                + ": "
                                + rootCause.getMessage()
                );

        if (outboxEvent.attempts()
                >= maxAttempts) {

            outboxEventRepository
                    .markDead(
                            outboxEvent.id(),
                            error
                    );

            log.error(
                    "Outbox event {} moved to DEAD after {} attempts",
                    outboxEvent.id(),
                    outboxEvent.attempts(),
                    rootCause
            );

            return;
        }

        Duration retryDelay =
                calculateRetryDelay(
                        outboxEvent.attempts()
                );

        Instant nextAttempt =
                Instant.now()
                        .plus(retryDelay);

        outboxEventRepository
                .markFailed(
                        outboxEvent.id(),
                        nextAttempt,
                        error
                );

        log.warn(
                "Failed to publish outbox event {}. Retrying in {} seconds. Attempt={}",
                outboxEvent.id(),
                retryDelay.toSeconds(),
                outboxEvent.attempts(),
                rootCause
        );
    }

    private Duration calculateRetryDelay(
            int attempts) {

        int exponent =
                Math.min(
                        Math.max(
                                attempts - 1,
                                0
                        ),
                        6
                );

        long seconds =
                Math.min(
                        60,
                        1L << exponent
                );

        return Duration.ofSeconds(
                seconds
        );
    }

    private Throwable rootCause(
            Exception exception) {

        if (exception
                instanceof ExecutionException executionException
                && executionException.getCause() != null) {

            return executionException
                    .getCause();
        }

        return exception;
    }

    private String truncate(
            String value) {

        if (value == null) {
            return null;
        }

        if (value.length()
                <= MAX_ERROR_LENGTH) {

            return value;
        }

        return value.substring(
                0,
                MAX_ERROR_LENGTH
        );
    }
}