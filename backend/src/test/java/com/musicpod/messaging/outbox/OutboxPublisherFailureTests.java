package com.musicpod.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.musicpod.messaging.event.PlaybackRecordedEvent;

import tools.jackson.databind.json.JsonMapper;
import static org.mockito.Mockito.reset;

import java.time.temporal.ChronoUnit;

@Testcontainers
@SpringBootTest(
        properties = {
                /*
                 * Disable the Spring-managed publisher/consumers.
                 *
                 * We will manually invoke OutboxPublisher
                 * so the test is deterministic.
                 */
                "app.kafka.enabled=false",

                /*
                 * Prevent AI auto-configuration from requiring
                 * a real credential during context startup.
                 */
                "spring.ai.openai.api-key=test-key"
        }
)
class OutboxPublisherFailureTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse(
                            "postgres:17-alpine"
                    )
            )
                    .withDatabaseName(
                            "musicpod"
                    )
                    .withUsername(
                            "musicpod"
                    )
                    .withPassword(
                            "musicpod"
                    );

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    /*
     * This is the failure point we're controlling.
     *
     * There is intentionally NO Kafka container in
     * this test.
     */
    @MockitoBean
    private KafkaTemplate<Object, Object> kafkaTemplate;

    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {

        jdbcTemplate.update(
                "DELETE FROM outbox_events"
        );

        /*
         * Construct the real publisher manually.
         *
         * batchSize = 25
         * maxAttempts = 20
         * sendTimeoutSeconds = 10
         */
        outboxPublisher =
                new OutboxPublisher(
                        outboxEventRepository,
                        kafkaTemplate,
                        jsonMapper,
                        25,
                        20,
                        10
                );
    }

    @Test
    void failedKafkaPublishLeavesEventPendingForRetry() {

        UUID eventId =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        UUID trackId =
                UUID.randomUUID();

        PlaybackRecordedEvent event =
                new PlaybackRecordedEvent(
                        eventId,
                        userId,
                        trackId,
                        45_000,
                        Instant.now(),
                        1
                );

        /*
         * ------------------------------------------------
         * 1. Put a real event into the outbox
         * ------------------------------------------------
         */
        outboxService.enqueue(
                event
        );

        OutboxRow before =
                findOutboxRow(
                        eventId
                );

        assertThat(
                before.status()
        ).isEqualTo(
                "PENDING"
        );

        assertThat(
                before.attempts()
        ).isZero();

        assertThat(
                before.lastError()
        ).isNull();

        assertThat(
                before.publishedAt()
        ).isNull();

        /*
         * ------------------------------------------------
         * 2. Simulate Kafka being unavailable
         * ------------------------------------------------
         */
        CompletableFuture<SendResult<Object, Object>>
                failedSend =
                new CompletableFuture<>();

        failedSend.completeExceptionally(
                new RuntimeException(
                        "simulated Kafka outage"
                )
        );

        when(
                kafkaTemplate.send(
                        anyString(),
                        any(),
                        any()
                )
        ).thenReturn(
                failedSend
        );

        /*
         * ------------------------------------------------
         * 3. Run the REAL outbox publisher
         * ------------------------------------------------
         */
        outboxPublisher.publishPendingEvents();

        /*
         * ------------------------------------------------
         * 4. Verify failure recovery
         * ------------------------------------------------
         */
        OutboxRow after =
                findOutboxRow(
                        eventId
                );

        /*
         * Important:
         *
         * The event was NOT deleted.
         * It was NOT incorrectly marked PUBLISHED.
         *
         * It has returned to PENDING so a later
         * poll can retry it.
         */
        assertThat(
                after.status()
        ).isEqualTo(
                "PENDING"
        );

        /*
         * claimBatch() increments attempts before
         * trying Kafka.
         */
        assertThat(
                after.attempts()
        ).isEqualTo(
                1
        );

        /*
         * Failed events must release their processing
         * lock so they can eventually be claimed again.
         */
        assertThat(
                after.lockedAt()
        ).isNull();

        /*
         * The retry must be scheduled later than the
         * original available time.
         */
        assertThat(
                after.availableAt()
        ).isAfter(
                before.availableAt()
        );

        /*
         * Operational debugging information is retained.
         */
        assertThat(
                after.lastError()
        )
                .contains(
                        "RuntimeException"
                )
                .contains(
                        "simulated Kafka outage"
                );

        /*
         * Definitely wasn't falsely acknowledged.
         */
        assertThat(
                after.publishedAt()
        ).isNull();
    }

    private OutboxRow findOutboxRow(
            UUID eventId) {

        return jdbcTemplate.queryForObject(
                """
                SELECT
                    status,
                    attempts,
                    available_at,
                    locked_at,
                    last_error,
                    published_at
                FROM outbox_events
                WHERE id = ?
                """,
                (resultSet, rowNumber) ->
                        new OutboxRow(
                                resultSet.getString(
                                        "status"
                                ),
                                resultSet.getInt(
                                        "attempts"
                                ),
                                toInstant(
                                        resultSet.getTimestamp(
                                                "available_at"
                                        )
                                ),
                                toInstant(
                                        resultSet.getTimestamp(
                                                "locked_at"
                                        )
                                ),
                                resultSet.getString(
                                        "last_error"
                                ),
                                toInstant(
                                        resultSet.getTimestamp(
                                                "published_at"
                                        )
                                )
                        ),
                eventId
        );
    }
    
    @Test
    void failedPublishIsRetriedAndEventuallyPublished() {

        UUID eventId =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        UUID trackId =
                UUID.randomUUID();

        PlaybackRecordedEvent event =
                new PlaybackRecordedEvent(
                        eventId,
                        userId,
                        trackId,
                        45_000,
                        Instant.now(),
                        1
                );

        outboxService.enqueue(
                event
        );

        /*
         * ------------------------------------------------
         * First attempt fails
         * ------------------------------------------------
         */

        CompletableFuture<SendResult<Object, Object>>
                failedSend =
                new CompletableFuture<>();

        failedSend.completeExceptionally(
                new RuntimeException(
                        "temporary Kafka outage"
                )
        );

        when(
                kafkaTemplate.send(
                        anyString(),
                        any(),
                        any()
                )
        ).thenReturn(
                failedSend
        );

        outboxPublisher.publishPendingEvents();

        OutboxRow afterFailure =
                findOutboxRow(
                        eventId
                );

        assertThat(
                afterFailure.status()
        ).isEqualTo(
                "PENDING"
        );

        assertThat(
                afterFailure.attempts()
        ).isEqualTo(
                1
        );

        assertThat(
                afterFailure.lastError()
        ).contains(
                "temporary Kafka outage"
        );

        /*
         * The publisher deliberately pushed available_at
         * into the future.
         *
         * For this test, move it back so we don't actually
         * wait for the retry delay.
         */
        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET available_at =
                    CURRENT_TIMESTAMP - INTERVAL '1 second'
                WHERE id = ?
                """,
                eventId
        );

        /*
         * ------------------------------------------------
         * Second attempt succeeds
         * ------------------------------------------------
         */

        CompletableFuture<SendResult<Object, Object>>
                successfulSend =
                CompletableFuture.completedFuture(
                        null
                );

        when(
                kafkaTemplate.send(
                        anyString(),
                        any(),
                        any()
                )
        ).thenReturn(
                successfulSend
        );

        outboxPublisher.publishPendingEvents();

        OutboxRow afterSuccess =
                findOutboxRow(
                        eventId
                );

        assertThat(
                afterSuccess.status()
        ).isEqualTo(
                "PUBLISHED"
        );

        /*
         * claimBatch incremented attempts once per claim.
         *
         * attempt 1 = failure
         * attempt 2 = success
         */
        assertThat(
                afterSuccess.attempts()
        ).isEqualTo(
                2
        );

        /*
         * markPublished() clears the previous error.
         */
        assertThat(
                afterSuccess.lastError()
        ).isNull();

        assertThat(
                afterSuccess.lockedAt()
        ).isNull();

        assertThat(
                afterSuccess.publishedAt()
        ).isNotNull();
    }
    
    @Test
    void repeatedKafkaFailuresEventuallyMoveEventToDead() {

        UUID eventId =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        UUID trackId =
                UUID.randomUUID();

        PlaybackRecordedEvent event =
                new PlaybackRecordedEvent(
                        eventId,
                        userId,
                        trackId,
                        45_000,
                        Instant.now(),
                        1
                );

        outboxService.enqueue(
                event
        );

        /*
         * Use a smaller maxAttempts for this test.
         *
         * Production can remain at 20.
         *
         * We only care about proving the behavior
         * when the configured threshold is reached.
         */
        OutboxPublisher publisherWithThreeAttempts =
                new OutboxPublisher(
                        outboxEventRepository,
                        kafkaTemplate,
                        jsonMapper,
                        25,
                        3,
                        10
                );

        CompletableFuture<SendResult<Object, Object>>
                failedSend =
                new CompletableFuture<>();

        failedSend.completeExceptionally(
                new RuntimeException(
                        "Kafka remains unavailable"
                )
        );

        when(
                kafkaTemplate.send(
                        anyString(),
                        any(),
                        any()
                )
        ).thenReturn(
                failedSend
        );

        /*
         * ------------------------------------------------
         * Attempt 1
         * ------------------------------------------------
         */
        publisherWithThreeAttempts
                .publishPendingEvents();

        OutboxRow afterFirstFailure =
                findOutboxRow(
                        eventId
                );

        assertThat(
                afterFirstFailure.status()
        ).isEqualTo(
                "PENDING"
        );

        assertThat(
                afterFirstFailure.attempts()
        ).isEqualTo(
                1
        );

        makeImmediatelyAvailable(
                eventId
        );

        /*
         * ------------------------------------------------
         * Attempt 2
         * ------------------------------------------------
         */
        publisherWithThreeAttempts
                .publishPendingEvents();

        OutboxRow afterSecondFailure =
                findOutboxRow(
                        eventId
                );

        assertThat(
                afterSecondFailure.status()
        ).isEqualTo(
                "PENDING"
        );

        assertThat(
                afterSecondFailure.attempts()
        ).isEqualTo(
                2
        );

        makeImmediatelyAvailable(
                eventId
        );

        /*
         * ------------------------------------------------
         * Attempt 3
         *
         * maxAttempts == 3
         *
         * claimBatch increments attempts to 3.
         * Kafka fails again.
         *
         * handleFailure now sees:
         *
         * attempts >= maxAttempts
         *
         * and moves the event to DEAD.
         * ------------------------------------------------
         */
        publisherWithThreeAttempts
                .publishPendingEvents();

        OutboxRow afterThirdFailure =
                findOutboxRow(
                        eventId
                );

        assertThat(
                afterThirdFailure.status()
        ).isEqualTo(
                "DEAD"
        );

        assertThat(
                afterThirdFailure.attempts()
        ).isEqualTo(
                3
        );

        assertThat(
                afterThirdFailure.lockedAt()
        ).isNull();

        assertThat(
                afterThirdFailure.publishedAt()
        ).isNull();

        assertThat(
                afterThirdFailure.lastError()
        )
                .contains(
                        "RuntimeException"
                )
                .contains(
                        "Kafka remains unavailable"
                );

        /*
         * ------------------------------------------------
         * Important additional assertion:
         *
         * DEAD events must no longer be claimable.
         * ------------------------------------------------
         */
        publisherWithThreeAttempts
                .publishPendingEvents();

        OutboxRow afterAnotherPoll =
                findOutboxRow(
                        eventId
                );

        assertThat(
                afterAnotherPoll.status()
        ).isEqualTo(
                "DEAD"
        );

        assertThat(
                afterAnotherPoll.attempts()
        ).isEqualTo(
                3
        );
    }
    
    private void makeImmediatelyAvailable(
            UUID eventId) {

        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET available_at =
                    CURRENT_TIMESTAMP - INTERVAL '1 second'
                WHERE id = ?
                """,
                eventId
        );
    }
    
    
    @Test
    void newerEventForSameKeyWaitsForOlderEventButOtherKeysContinue() {

        UUID trackA =
                UUID.randomUUID();

        UUID trackB =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        UUID eventA1Id =
                UUID.randomUUID();

        UUID eventA2Id =
                UUID.randomUUID();

        UUID eventB1Id =
                UUID.randomUUID();

        /*
         * ------------------------------------------------
         * A1 - older event for Track A
         * ------------------------------------------------
         */
        PlaybackRecordedEvent eventA1 =
                new PlaybackRecordedEvent(
                        eventA1Id,
                        userId,
                        trackA,
                        30_000,
                        Instant.now(),
                        1
                );

        /*
         * ------------------------------------------------
         * A2 - newer event for SAME Track A
         * ------------------------------------------------
         */
        PlaybackRecordedEvent eventA2 =
                new PlaybackRecordedEvent(
                        eventA2Id,
                        userId,
                        trackA,
                        45_000,
                        Instant.now(),
                        1
                );

        /*
         * ------------------------------------------------
         * B1 - independent Track B
         * ------------------------------------------------
         */
        PlaybackRecordedEvent eventB1 =
                new PlaybackRecordedEvent(
                        eventB1Id,
                        userId,
                        trackB,
                        60_000,
                        Instant.now(),
                        1
                );

        outboxService.enqueue(
                eventA1
        );

        outboxService.enqueue(
                eventA2
        );

        outboxService.enqueue(
                eventB1
        );

        /*
         * Make ordering completely deterministic.
         *
         * We don't want this test depending on whether
         * PostgreSQL gave two inserts identical timestamps.
         */
        Instant baseTime =
                Instant.now()
                        .minus(
                                10,
                                ChronoUnit.SECONDS
                        );

        setCreatedAt(
                eventA1Id,
                baseTime
        );

        setCreatedAt(
                eventA2Id,
                baseTime.plusSeconds(
                        1
                )
        );

        setCreatedAt(
                eventB1Id,
                baseTime.plusSeconds(
                        2
                )
        );

        /*
         * ====================================================
         * FIRST POLL
         *
         * A1 -> Kafka failure
         * A2 -> must NOT be claimed
         * B1 -> succeeds
         * ====================================================
         */

        when(
                kafkaTemplate.send(
                        anyString(),
                        any(),
                        any()
                )
        ).thenAnswer(
                invocation -> {

                    Object messageKey =
                            invocation.getArgument(
                                    1
                            );

                    if (trackA
                            .toString()
                            .equals(
                                    messageKey
                            )) {

                        CompletableFuture<
                                SendResult<Object, Object>>
                                failure =
                                new CompletableFuture<>();

                        failure.completeExceptionally(
                                new RuntimeException(
                                        "Track A Kafka failure"
                                )
                        );

                        return failure;
                    }

                    return CompletableFuture
                            .completedFuture(
                                    null
                            );
                }
        );

        outboxPublisher
                .publishPendingEvents();

        OutboxRow a1AfterFailure =
                findOutboxRow(
                        eventA1Id
                );

        OutboxRow a2AfterFirstPoll =
                findOutboxRow(
                        eventA2Id
                );

        OutboxRow b1AfterFirstPoll =
                findOutboxRow(
                        eventB1Id
                );

        /*
         * A1 was attempted and failed.
         */
        assertThat(
                a1AfterFailure.status()
        ).isEqualTo(
                "PENDING"
        );

        assertThat(
                a1AfterFailure.attempts()
        ).isEqualTo(
                1
        );

        assertThat(
                a1AfterFailure.lastError()
        ).contains(
                "Track A Kafka failure"
        );

        /*
         * Critical assertion:
         *
         * A2 was never even claimed.
         *
         * If ordering were broken, attempts would
         * already be 1.
         */
        assertThat(
                a2AfterFirstPoll.status()
        ).isEqualTo(
                "PENDING"
        );

        assertThat(
                a2AfterFirstPoll.attempts()
        ).isZero();

        /*
         * Track B is unrelated.
         *
         * Failure for Track A must not block B1.
         */
        assertThat(
                b1AfterFirstPoll.status()
        ).isEqualTo(
                "PUBLISHED"
        );

        assertThat(
                b1AfterFirstPoll.attempts()
        ).isEqualTo(
                1
        );

        assertThat(
                b1AfterFirstPoll.publishedAt()
        ).isNotNull();

        /*
         * ====================================================
         * SECOND POLL
         *
         * Kafka has recovered.
         *
         * A1 is made eligible again.
         * A1 should publish.
         *
         * A2 should STILL not be claimed in the same
         * database claim operation because A1 was still
         * unresolved at claim time.
         * ====================================================
         */

        makeImmediatelyAvailable(
                eventA1Id
        );

        reset(
                kafkaTemplate
        );

        when(
                kafkaTemplate.send(
                        anyString(),
                        any(),
                        any()
                )
        ).thenReturn(
                CompletableFuture
                        .completedFuture(
                                null
                        )
        );

        outboxPublisher
                .publishPendingEvents();

        OutboxRow a1AfterRecovery =
                findOutboxRow(
                        eventA1Id
                );

        OutboxRow a2AfterSecondPoll =
                findOutboxRow(
                        eventA2Id
                );

        assertThat(
                a1AfterRecovery.status()
        ).isEqualTo(
                "PUBLISHED"
        );

        assertThat(
                a1AfterRecovery.attempts()
        ).isEqualTo(
                2
        );

        /*
         * This is subtle but important.
         *
         * claimBatch() made its decision BEFORE A1
         * became PUBLISHED.
         *
         * Therefore A2 must still be untouched.
         */
        assertThat(
                a2AfterSecondPoll.status()
        ).isEqualTo(
                "PENDING"
        );

        assertThat(
                a2AfterSecondPoll.attempts()
        ).isZero();

        /*
         * ====================================================
         * THIRD POLL
         *
         * A1 is now PUBLISHED.
         *
         * Nothing older blocks A2 anymore.
         * ====================================================
         */

        outboxPublisher
                .publishPendingEvents();

        OutboxRow a2AfterThirdPoll =
                findOutboxRow(
                        eventA2Id
                );

        assertThat(
                a2AfterThirdPoll.status()
        ).isEqualTo(
                "PUBLISHED"
        );

        assertThat(
                a2AfterThirdPoll.attempts()
        ).isEqualTo(
                1
        );

        assertThat(
                a2AfterThirdPoll.publishedAt()
        ).isNotNull();
    }

    private void setCreatedAt(
            UUID eventId,
            Instant createdAt) {

        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET created_at = ?
                WHERE id = ?
                """,
                Timestamp.from(
                        createdAt
                ),
                eventId
        );
    }
    
    private Instant toInstant(
            Timestamp timestamp) {

        return timestamp == null
                ? null
                : timestamp.toInstant();
    }

    private record OutboxRow(
            String status,
            int attempts,
            Instant availableAt,
            Instant lockedAt,
            String lastError,
            Instant publishedAt) {
    }
    
    @Test
    void staleProcessingEventIsRecoveredAfterPublisherCrash() {

        UUID eventId =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        UUID trackId =
                UUID.randomUUID();

        PlaybackRecordedEvent event =
                new PlaybackRecordedEvent(
                        eventId,
                        userId,
                        trackId,
                        45_000,
                        Instant.now(),
                        1
                );

        /*
         * ------------------------------------------------
         * 1. Create normal outbox event
         * ------------------------------------------------
         */
        outboxService.enqueue(
                event
        );

        OutboxRow initial =
                findOutboxRow(
                        eventId
                );

        assertThat(
                initial.status()
        ).isEqualTo(
                "PENDING"
        );

        assertThat(
                initial.attempts()
        ).isZero();

        /*
         * ------------------------------------------------
         * 2. Simulate publisher claiming the message
         * ------------------------------------------------
         *
         * Normally OutboxPublisher calls this.
         *
         * We call claimBatch directly and then deliberately
         * stop processing.
         *
         * This represents:
         *
         * claim successful
         *      ↓
         * application crashes
         */
        List<OutboxEvent> claimed =
                outboxEventRepository
                        .claimBatch(
                                25
                        );

        assertThat(
                claimed
        ).hasSize(
                1
        );

        assertThat(
                claimed.getFirst().id()
        ).isEqualTo(
                eventId
        );

        OutboxRow afterClaim =
                findOutboxRow(
                        eventId
                );

        assertThat(
                afterClaim.status()
        ).isEqualTo(
                "PROCESSING"
        );

        assertThat(
                afterClaim.attempts()
        ).isEqualTo(
                1
        );

        assertThat(
                afterClaim.lockedAt()
        ).isNotNull();

        /*
         * ------------------------------------------------
         * 💥 Publisher crashes here
         * ------------------------------------------------
         *
         * No:
         *
         * markPublished()
         *
         * and no:
         *
         * markFailed()
         *
         * Therefore the row remains PROCESSING.
         */

        /*
         * ------------------------------------------------
         * 3. A normal poll should NOT immediately
         *    reclaim it.
         * ------------------------------------------------
         */
        List<OutboxEvent> immediatelyClaimable =
                outboxEventRepository
                        .claimBatch(
                                25
                        );

        assertThat(
                immediatelyClaimable
        ).isEmpty();

        OutboxRow stillProcessing =
                findOutboxRow(
                        eventId
                );

        assertThat(
                stillProcessing.status()
        ).isEqualTo(
                "PROCESSING"
        );

        assertThat(
                stillProcessing.attempts()
        ).isEqualTo(
                1
        );

        /*
         * ------------------------------------------------
         * 4. Simulate lock becoming stale
         * ------------------------------------------------
         *
         * Production waits 15 minutes.
         *
         * Don't actually wait 15 minutes in the test.
         */
        jdbcTemplate.update(
                """
                UPDATE outbox_events
                SET locked_at =
                    CURRENT_TIMESTAMP - INTERVAL '16 minutes'
                WHERE id = ?
                """,
                eventId
        );

        /*
         * Kafka is now healthy.
         */
        when(
                kafkaTemplate.send(
                        anyString(),
                        any(),
                        any()
                )
        ).thenReturn(
                CompletableFuture
                        .completedFuture(
                                null
                        )
        );

        /*
         * ------------------------------------------------
         * 5. New publisher poll recovers stale message
         * ------------------------------------------------
         */
        outboxPublisher
                .publishPendingEvents();

        OutboxRow recovered =
                findOutboxRow(
                        eventId
                );

        /*
         * Event was reclaimed and successfully published.
         */
        assertThat(
                recovered.status()
        ).isEqualTo(
                "PUBLISHED"
        );

        /*
         * First claim before crash:
         *
         * attempts = 1
         *
         * Recovery claim:
         *
         * attempts = 2
         */
        assertThat(
                recovered.attempts()
        ).isEqualTo(
                2
        );

        assertThat(
                recovered.lockedAt()
        ).isNull();

        assertThat(
                recovered.lastError()
        ).isNull();

        assertThat(
                recovered.publishedAt()
        ).isNotNull();
    }
    
    @Test
    void concurrentPublishersDoNotPublishSameEventTwice()
            throws Exception {

        UUID eventId =
                UUID.randomUUID();

        UUID userId =
                UUID.randomUUID();

        UUID trackId =
                UUID.randomUUID();

        PlaybackRecordedEvent event =
                new PlaybackRecordedEvent(
                        eventId,
                        userId,
                        trackId,
                        45_000,
                        Instant.now(),
                        1
                );

        outboxService.enqueue(
                event
        );

        /*
         * Simulate two MusicPod instances.
         */
        OutboxPublisher publisherOne =
                new OutboxPublisher(
                        outboxEventRepository,
                        kafkaTemplate,
                        jsonMapper,
                        1,
                        20,
                        10
                );

        OutboxPublisher publisherTwo =
                new OutboxPublisher(
                        outboxEventRepository,
                        kafkaTemplate,
                        jsonMapper,
                        1,
                        20,
                        10
                );

        /*
         * Publisher #1 will claim the event and reach Kafka,
         * but we deliberately prevent Kafka from completing.
         *
         * That leaves the DB row in:
         *
         * PROCESSING
         * locked_at != null
         */
        CompletableFuture<SendResult<Object, Object>>
                blockedKafkaSend =
                new CompletableFuture<>();

        CountDownLatch firstPublisherReachedKafka =
                new CountDownLatch(
                        1
                );

        when(
                kafkaTemplate.send(
                        anyString(),
                        any(),
                        any()
                )
        ).thenAnswer(
                invocation -> {

                    firstPublisherReachedKafka
                            .countDown();

                    return blockedKafkaSend;
                }
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(
                        2
                );

        try {

            /*
             * ------------------------------------------------
             * Publisher #1 starts
             * ------------------------------------------------
             */
            Future<?> first =
                    executor.submit(
                            publisherOne::publishPendingEvents
                    );

            /*
             * Wait until publisher #1 has:
             *
             * 1. claimed the DB row
             * 2. reached Kafka
             *
             * At this point it is blocked waiting for
             * the Kafka future.
             */
            boolean reachedKafka =
                    firstPublisherReachedKafka
                            .await(
                                    5,
                                    TimeUnit.SECONDS
                            );

            assertThat(
                    reachedKafka
            ).isTrue();

            OutboxRow whileFirstPublisherIsWorking =
                    findOutboxRow(
                            eventId
                    );

            assertThat(
                    whileFirstPublisherIsWorking.status()
            ).isEqualTo(
                    "PROCESSING"
            );

            assertThat(
                    whileFirstPublisherIsWorking.attempts()
            ).isEqualTo(
                    1
            );

            assertThat(
                    whileFirstPublisherIsWorking.lockedAt()
            ).isNotNull();

            /*
             * ------------------------------------------------
             * Publisher #2 runs concurrently
             * ------------------------------------------------
             *
             * It must NOT claim this event because publisher
             * #1 currently owns it.
             */
            Future<?> second =
                    executor.submit(
                            publisherTwo::publishPendingEvents
                    );

            second.get(
                    5,
                    TimeUnit.SECONDS
            );

            /*
             * Nothing should have changed.
             *
             * If publisher #2 had claimed it too,
             * attempts would have become 2.
             */
            OutboxRow afterSecondPublisher =
                    findOutboxRow(
                            eventId
                    );

            assertThat(
                    afterSecondPublisher.status()
            ).isEqualTo(
                    "PROCESSING"
            );

            assertThat(
                    afterSecondPublisher.attempts()
            ).isEqualTo(
                    1
            );

            /*
             * ------------------------------------------------
             * Kafka finally acknowledges publisher #1
             * ------------------------------------------------
             */
            blockedKafkaSend.complete(
                    null
            );

            first.get(
                    5,
                    TimeUnit.SECONDS
            );

            OutboxRow finalState =
                    findOutboxRow(
                            eventId
                    );

            assertThat(
                    finalState.status()
            ).isEqualTo(
                    "PUBLISHED"
            );

            assertThat(
                    finalState.attempts()
            ).isEqualTo(
                    1
            );

            assertThat(
                    finalState.publishedAt()
            ).isNotNull();

            assertThat(
                    finalState.lockedAt()
            ).isNull();

            /*
             * Most important assertion:
             *
             * only ONE Kafka send happened.
             */
            verify(
                    kafkaTemplate,
                    times(
                            1
                    )
            ).send(
                    anyString(),
                    any(),
                    any()
            );

        } finally {

            executor.shutdownNow();
        }
    }
}