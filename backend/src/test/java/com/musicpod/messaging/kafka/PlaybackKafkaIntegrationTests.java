package com.musicpod.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Testcontainers
@SpringBootTest(
        properties = {
                "app.kafka.enabled=true",
                "app.outbox.poll-interval-ms=100",
                "app.outbox.initial-delay-ms=100"
        }
)
@AutoConfigureMockMvc
class PlaybackKafkaIntegrationTests {

    private static final Duration
            ASYNC_TIMEOUT =
            Duration.ofSeconds(15);

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

    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(
                    DockerImageName.parse(
                            "apache/kafka:4.3.1"
                    )
            );

    @DynamicPropertySource
    static void kafkaProperties(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.kafka.bootstrap-servers",
                KAFKA::getBootstrapServers
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAnalyticsTables() {

        /*
         * Delete derived/event-processing data first.
         *
         * We do not wipe catalog/user tables here
         * because each test creates its own uniquely
         * named fixture.
         */

        jdbcTemplate.update(
                "DELETE FROM processed_playback_events"
        );

        jdbcTemplate.update(
                "DELETE FROM track_play_stats"
        );

        /*
         * M13 added the hourly projection.
         *
         * Keep this cleanup if V10 is already
         * present in your branch.
         */
        jdbcTemplate.update(
                "DELETE FROM track_play_stats_hourly"
        );

        jdbcTemplate.update(
                "DELETE FROM outbox_events"
        );

        jdbcTemplate.update(
                "DELETE FROM playback_events"
        );
    }

    @Test
    void playbackFlowsThroughOutboxKafkaAndAnalytics()
            throws Exception {

        TestFixture fixture =
                createFixture();

        int playedMs =
                120_000;

        MvcResult playbackResult =
                mockMvc.perform(
                                post(
                                        "/api/v1/me/playback-events"
                                )
                                        .header(
                                                "Authorization",
                                                "Bearer "
                                                        + fixture.accessToken()
                                        )
                                        .contentType(
                                                "application/json"
                                        )
                                        .content(
                                                """
                                                {
                                                  "trackId": "%s",
                                                  "playedMs": %d
                                                }
                                                """.formatted(
                                                        fixture.trackId(),
                                                        playedMs
                                                )
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn();

        String playbackBody =
                playbackResult
                        .getResponse()
                        .getContentAsString();

        UUID playbackEventId =
                UUID.fromString(
                        jsonMapper
                                .readTree(
                                        playbackBody
                                )
                                .get("id")
                                .asText()
                );

        /*
         * The HTTP request only guarantees that
         * playback_events + outbox committed.
         *
         * Kafka processing is asynchronous, so
         * poll the projection rather than sleeping
         * for an arbitrary fixed amount of time.
         */
        TrackStats stats =
                awaitTrackStats(
                        fixture.trackId(),
                        1,
                        playedMs
                );

        assertThat(
                stats.playCount()
        ).isEqualTo(
                1
        );

        assertThat(
                stats.totalPlayedMs()
        ).isEqualTo(
                playedMs
        );

        /*
         * Verify the consumer's idempotency marker.
         *
         * PlaybackRecordedEvent.eventId is the
         * PlaybackEvent UUID.
         */
        Integer processed =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM processed_playback_events
                        WHERE event_id = ?
                        """,
                        Integer.class,
                        playbackEventId
                );

        assertThat(
                processed
        ).isEqualTo(
                1
        );

        /*
         * The transactional-outbox relay should
         * eventually mark the message published.
         */
        awaitOutboxPublished(
                playbackEventId
        );

        String outboxStatus =
                jdbcTemplate.queryForObject(
                        """
                        SELECT status
                        FROM outbox_events
                        WHERE id = ?
                        """,
                        String.class,
                        playbackEventId
                );

        assertThat(
                outboxStatus
        ).isEqualTo(
                "PUBLISHED"
        );
    }

    @Test
    void multiplePlaybackEventsAreAggregated()
            throws Exception {

        TestFixture fixture =
                createFixture();

        recordPlayback(
                fixture,
                30_000
        );

        recordPlayback(
                fixture,
                45_000
        );

        recordPlayback(
                fixture,
                60_000
        );

        TrackStats stats =
                awaitTrackStats(
                        fixture.trackId(),
                        3,
                        135_000
                );

        assertThat(
                stats.playCount()
        ).isEqualTo(
                3
        );

        assertThat(
                stats.totalPlayedMs()
        ).isEqualTo(
                135_000
        );

        Integer processedEvents =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM processed_playback_events
                        """,
                        Integer.class
                );

        assertThat(
                processedEvents
        ).isEqualTo(
                3
        );
    }

    private void recordPlayback(
            TestFixture fixture,
            int playedMs)
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/me/playback-events"
                        )
                                .header(
                                        "Authorization",
                                        "Bearer "
                                                + fixture.accessToken()
                                )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "trackId": "%s",
                                          "playedMs": %d
                                        }
                                        """.formatted(
                                                fixture.trackId(),
                                                playedMs
                                        )
                                )
                )
                .andExpect(
                        status().isCreated()
                );
    }

    /*
     * Poll until Kafka has asynchronously updated
     * track_play_stats.
     */
    private TrackStats awaitTrackStats(
            UUID trackId,
            long expectedPlayCount,
            long expectedPlayedMs)
            throws InterruptedException {

        long deadline =
                System.nanoTime()
                        + ASYNC_TIMEOUT.toNanos();

        TrackStats latest =
                null;

        while (System.nanoTime()
                < deadline) {

            latest =
                    findTrackStats(
                            trackId
                    );

            if (latest != null
                    && latest.playCount()
                    == expectedPlayCount
                    && latest.totalPlayedMs()
                    == expectedPlayedMs) {

                return latest;
            }

            Thread.sleep(
                    100
            );
        }

        throw new AssertionError(
                "Timed out waiting for playback analytics. "
                        + "trackId="
                        + trackId
                        + ", expectedPlayCount="
                        + expectedPlayCount
                        + ", expectedPlayedMs="
                        + expectedPlayedMs
                        + ", latest="
                        + latest
        );
    }

    private TrackStats findTrackStats(
            UUID trackId) {

        List<Map<String, Object>> rows =
                jdbcTemplate.queryForList(
                        """
                        SELECT
                            play_count,
                            total_played_ms
                        FROM track_play_stats
                        WHERE track_id = ?
                        """,
                        trackId
                );

        if (rows.isEmpty()) {
            return null;
        }

        Map<String, Object> row =
                rows.getFirst();

        return new TrackStats(
                (
                        (Number)
                                row.get(
                                        "play_count"
                                )
                ).longValue(),
                (
                        (Number)
                                row.get(
                                        "total_played_ms"
                                )
                ).longValue()
        );
    }

    private void awaitOutboxPublished(
            UUID eventId)
            throws InterruptedException {

        long deadline =
                System.nanoTime()
                        + ASYNC_TIMEOUT.toNanos();

        while (System.nanoTime()
                < deadline) {

            List<String> statuses =
                    jdbcTemplate.queryForList(
                            """
                            SELECT status
                            FROM outbox_events
                            WHERE id = ?
                            """,
                            String.class,
                            eventId
                    );

            if (!statuses.isEmpty()
                    && "PUBLISHED".equals(
                            statuses.getFirst()
                    )) {

                return;
            }

            Thread.sleep(
                    100
            );
        }

        throw new AssertionError(
                "Timed out waiting for outbox event "
                        + eventId
                        + " to become PUBLISHED"
        );
    }

    /*
     * Creates:
     *
     * user
     *   ↓
     * artist
     *   ↓
     * album
     *   ↓
     * track
     *
     * through the real HTTP APIs.
     */
    private TestFixture createFixture()
            throws Exception {

        String suffix =
                UUID.randomUUID()
                        .toString()
                        .substring(
                                0,
                                8
                        );

        String email =
                "playback-kafka-"
                        + suffix
                        + "@example.com";

        String password =
                "musicpod123!";

        register(
                email,
                password,
                "Kafka Listener"
        );

        String accessToken =
                login(
                        email,
                        password
                );

        UUID artistId =
                createArtist(
                        accessToken,
                        "Kafka Artist "
                                + suffix
                );

        UUID albumId =
                createAlbum(
                        accessToken,
                        artistId,
                        "Kafka Album "
                                + suffix
                );

        UUID trackId =
                createTrack(
                        accessToken,
                        albumId,
                        "Kafka Track "
                                + suffix
                );

        return new TestFixture(
                accessToken,
                trackId
        );
    }

    private void register(
            String email,
            String password,
            String displayName)
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/v1/auth/register"
                        )
                                .contentType(
                                        "application/json"
                                )
                                .content(
                                        """
                                        {
                                          "email": "%s",
                                          "password": "%s",
                                          "displayName": "%s"
                                        }
                                        """.formatted(
                                                email,
                                                password,
                                                displayName
                                        )
                                )
                )
                .andExpect(
                        status().isCreated()
                );
    }

    private String login(
            String email,
            String password)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/v1/auth/login"
                                )
                                        .contentType(
                                                "application/json"
                                        )
                                        .content(
                                                """
                                                {
                                                  "email": "%s",
                                                  "password": "%s"
                                                }
                                                """.formatted(
                                                        email,
                                                        password
                                                )
                                        )
                        )
                        .andExpect(
                                status().isOk()
                        )
                        .andReturn();

        return jsonMapper
                .readTree(
                        result
                                .getResponse()
                                .getContentAsString()
                )
                .get(
                        "accessToken"
                )
                .asText();
    }

    private UUID createArtist(
            String token,
            String name)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/v1/artists"
                                )
                                        .header(
                                                "Authorization",
                                                "Bearer "
                                                        + token
                                        )
                                        .contentType(
                                                "application/json"
                                        )
                                        .content(
                                                """
                                                {
                                                  "name": "%s"
                                                }
                                                """.formatted(
                                                        name
                                                )
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn();

        return responseId(
                result
        );
    }

    private UUID createAlbum(
            String token,
            UUID artistId,
            String title)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/v1/artists/{artistId}/albums",
                                        artistId
                                )
                                        .header(
                                                "Authorization",
                                                "Bearer "
                                                        + token
                                        )
                                        .contentType(
                                                "application/json"
                                        )
                                        .content(
                                                """
                                                {
                                                  "title": "%s",
                                                  "releaseDate": "2026-08-10"
                                                }
                                                """.formatted(
                                                        title
                                                )
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn();

        return responseId(
                result
        );
    }

    private UUID createTrack(
            String token,
            UUID albumId,
            String title)
            throws Exception {

        MvcResult result =
                mockMvc.perform(
                                post(
                                        "/api/v1/albums/{albumId}/tracks",
                                        albumId
                                )
                                        .header(
                                                "Authorization",
                                                "Bearer "
                                                        + token
                                        )
                                        .contentType(
                                                "application/json"
                                        )
                                        .content(
                                                """
                                                {
                                                  "title": "%s",
                                                  "trackNumber": 1,
                                                  "durationMs": 300000,
                                                  "explicit": false
                                                }
                                                """.formatted(
                                                        title
                                                )
                                        )
                        )
                        .andExpect(
                                status().isCreated()
                        )
                        .andReturn();

        return responseId(
                result
        );
    }

    private UUID responseId(
            MvcResult result) throws JacksonException, UnsupportedEncodingException {

        return UUID.fromString(
                jsonMapper
                        .readTree(
                                result
                                        .getResponse()
                                        .getContentAsString()
                        )
                        .get(
                                "id"
                        )
                        .asText()
        );
    }

    private record TestFixture(
            String accessToken,
            UUID trackId) {
    }

    private record TrackStats(
            long playCount,
            long totalPlayedMs) {
    }
}