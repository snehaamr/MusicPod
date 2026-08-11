package com.musicpod.messaging.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.musicpod.messaging.event.PlaybackRecordedEvent;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Testcontainers
@SpringBootTest(
        properties = {
                "app.kafka.enabled=false",
                "spring.ai.openai.api-key=test-key"
        }
)
class OutboxMetricsIntegrationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(
                    DockerImageName.parse(
                            "postgres:17-alpine"
                    )
            )
                    .withDatabaseName("musicpod")
                    .withUsername("musicpod")
                    .withPassword("musicpod");

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxEventRepository
            outboxEventRepository;

    @BeforeEach
    void setUp() {

        jdbcTemplate.update(
                "DELETE FROM outbox_events"
        );
    }

    @Test
    void outboxStateGaugesReflectDatabaseState() {

        /*
         * Initial state:
         *
         * No outbox records exist.
         */
        assertGaugeValue(
                "musicpod.outbox.pending",
                0.0
        );

        assertGaugeValue(
                "musicpod.outbox.processing",
                0.0
        );

        assertGaugeValue(
                "musicpod.outbox.dead",
                0.0
        );

        /*
         * Create an outbox event.
         *
         * enqueue() inserts it as:
         *
         * status = PENDING
         * attempts = 0
         */
        UUID eventId =
                UUID.randomUUID();

        PlaybackRecordedEvent event =
                new PlaybackRecordedEvent(
                        eventId,
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        45_000,
                        Instant.now(),
                        1
                );

        outboxService.enqueue(event);

        /*
         * PENDING:
         *
         * pending    = 1
         * processing = 0
         * dead       = 0
         */
        assertGaugeValue(
                "musicpod.outbox.pending",
                1.0
        );

        assertGaugeValue(
                "musicpod.outbox.processing",
                0.0
        );

        assertGaugeValue(
                "musicpod.outbox.dead",
                0.0
        );

        /*
         * Claim the event exactly the same way
         * OutboxPublisher does.
         *
         * claimBatch() changes:
         *
         * PENDING -> PROCESSING
         *
         * and increments attempts.
         */
        OutboxEvent claimedEvent =
                outboxEventRepository
                        .claimBatch(1)
                        .getFirst();

        /*
         * PROCESSING:
         *
         * pending    = 0
         * processing = 1
         * dead       = 0
         */
        assertGaugeValue(
                "musicpod.outbox.pending",
                0.0
        );

        assertGaugeValue(
                "musicpod.outbox.processing",
                1.0
        );

        assertGaugeValue(
                "musicpod.outbox.dead",
                0.0
        );

        /*
         * Simulate exhaustion of publish retries.
         *
         * We are testing metric state transitions
         * here, not retry policy itself.
         *
         * Retry behavior already belongs in the
         * OutboxPublisher failure tests.
         */
        outboxEventRepository.markDead(
                claimedEvent.id(),
                "simulated terminal failure"
        );

        /*
         * DEAD:
         *
         * pending    = 0
         * processing = 0
         * dead       = 1
         */
        assertGaugeValue(
                "musicpod.outbox.pending",
                0.0
        );

        assertGaugeValue(
                "musicpod.outbox.processing",
                0.0
        );

        assertGaugeValue(
                "musicpod.outbox.dead",
                1.0
        );
    }

    private void assertGaugeValue(
            String metricName,
            double expectedValue) {

        Gauge gauge =
                meterRegistry
                        .find(metricName)
                        .gauge();

        assertThat(gauge)
                .as(
                        "Gauge %s should be registered",
                        metricName
                )
                .isNotNull();

        assertThat(gauge.value())
                .as(
                        "Unexpected value for gauge %s",
                        metricName
                )
                .isEqualTo(expectedValue);
    }
}