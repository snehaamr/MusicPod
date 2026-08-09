package com.musicpod.messaging.outbox;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.musicpod.messaging.event.PlaybackRecordedEvent;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTests {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Mock
    private JsonMapper jsonMapper;

    private OutboxPublisher publisher;

    @BeforeEach
    void setUp() {

        publisher =
                new OutboxPublisher(
                        outboxEventRepository,
                        kafkaTemplate,
                        jsonMapper,
                        25,
                        3,
                        1
                );
    }

    @Test
    void emptyBatchDoesNothing() {

        when(
                outboxEventRepository.claimBatch(
                        25
                )
        ).thenReturn(
                List.of()
        );

        publisher.publishPendingEvents();

        verify(
                outboxEventRepository
        ).claimBatch(
                25
        );

        verifyNoInteractions(
                kafkaTemplate,
                jsonMapper
        );
    }

    @Test
    void successfulKafkaSendMarksEventPublished()
            throws Exception {

        OutboxEvent outboxEvent =
                eventWithAttempts(
                        1
                );

        PlaybackRecordedEvent payload =
                org.mockito.Mockito.mock(
                        PlaybackRecordedEvent.class
                );

        when(
                outboxEventRepository.claimBatch(
                        25
                )
        ).thenReturn(
                List.of(
                        outboxEvent
                )
        );

        when(
                jsonMapper.readValue(
                        outboxEvent.payload(),
                        PlaybackRecordedEvent.class
                )
        ).thenReturn(
                payload
        );

        CompletableFuture<SendResult<Object, Object>>
                future =
                CompletableFuture.completedFuture(
                        null
                );

        when(
                kafkaTemplate.send(
                        outboxEvent.topic(),
                        outboxEvent.messageKey(),
                        payload
                )
        ).thenReturn(
                future
        );

        publisher.publishPendingEvents();

        verify(
                outboxEventRepository
        ).markPublished(
                outboxEvent.id()
        );

        verify(
                outboxEventRepository,
                never()
        ).markFailed(
                any(),
                any(),
                any()
        );

        verify(
                outboxEventRepository,
                never()
        ).markDead(
                any(),
                any()
        );
    }

    @Test
    void failedKafkaSendSchedulesRetry()
            throws Exception {

        OutboxEvent outboxEvent =
                eventWithAttempts(
                        1
                );

        PlaybackRecordedEvent payload =
                org.mockito.Mockito.mock(
                        PlaybackRecordedEvent.class
                );

        when(
                outboxEventRepository.claimBatch(
                        25
                )
        ).thenReturn(
                List.of(
                        outboxEvent
                )
        );

        when(
                jsonMapper.readValue(
                        outboxEvent.payload(),
                        PlaybackRecordedEvent.class
                )
        ).thenReturn(
                payload
        );

        CompletableFuture<SendResult<Object, Object>>
                future =
                new CompletableFuture<>();

        future.completeExceptionally(
                new IllegalStateException(
                        "Kafka unavailable"
                )
        );

        when(
                kafkaTemplate.send(
                        outboxEvent.topic(),
                        outboxEvent.messageKey(),
                        payload
                )
        ).thenReturn(
                future
        );

        publisher.publishPendingEvents();

        verify(
                outboxEventRepository
        ).markFailed(
                eq(
                        outboxEvent.id()
                ),
                any(
                        Instant.class
                ),
                eq(
                        "IllegalStateException: Kafka unavailable"
                )
        );

        verify(
                outboxEventRepository,
                never()
        ).markPublished(
                outboxEvent.id()
        );

        verify(
                outboxEventRepository,
                never()
        ).markDead(
                any(),
                any()
        );
    }

    @Test
    void exhaustedEventMovesToDead()
            throws Exception {

        OutboxEvent outboxEvent =
                eventWithAttempts(
                        3
                );

        PlaybackRecordedEvent payload =
                org.mockito.Mockito.mock(
                        PlaybackRecordedEvent.class
                );

        when(
                outboxEventRepository.claimBatch(
                        25
                )
        ).thenReturn(
                List.of(
                        outboxEvent
                )
        );

        when(
                jsonMapper.readValue(
                        outboxEvent.payload(),
                        PlaybackRecordedEvent.class
                )
        ).thenReturn(
                payload
        );

        CompletableFuture<SendResult<Object, Object>>
                future =
                new CompletableFuture<>();

        future.completeExceptionally(
                new IllegalStateException(
                        "Kafka unavailable"
                )
        );

        when(
                kafkaTemplate.send(
                        outboxEvent.topic(),
                        outboxEvent.messageKey(),
                        payload
                )
        ).thenReturn(
                future
        );

        publisher.publishPendingEvents();

        verify(
                outboxEventRepository
        ).markDead(
                outboxEvent.id(),
                "IllegalStateException: Kafka unavailable"
        );

        verify(
                outboxEventRepository,
                never()
        ).markFailed(
                any(),
                any(),
                any()
        );

        verify(
                outboxEventRepository,
                never()
        ).markPublished(
                outboxEvent.id()
        );
    }

    private OutboxEvent eventWithAttempts(
            int attempts) {

        return new OutboxEvent(
                UUID.randomUUID(),
                "TRACK",
                UUID.randomUUID(),
                PlaybackRecordedEvent.EVENT_TYPE,
                "musicpod.playback.recorded.v1",
                UUID.randomUUID().toString(),
                "{}",
                attempts,
                Instant.now()
        );
    }
}