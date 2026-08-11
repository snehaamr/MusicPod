package com.musicpod.search.track;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch._types.OpenSearchException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.opensearch.client.opensearch._types.OpenSearchException;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import com.musicpod.catalog.album.Album;
import com.musicpod.catalog.album.AlbumRepository;
import com.musicpod.catalog.artist.Artist;
import com.musicpod.catalog.artist.ArtistRepository;
import com.musicpod.catalog.track.CreateTrackRequest;
import com.musicpod.catalog.track.TrackResponse;
import com.musicpod.catalog.track.TrackService;
import com.musicpod.catalog.track.UpdateTrackRequest;
import com.musicpod.search.SearchIndexNames;

@Testcontainers
@SpringBootTest(
        properties = {
                "app.kafka.enabled=true",
                "app.outbox.poll-interval-ms=100",
                "app.outbox.initial-delay-ms=100",

                /*
                 * Prevent Spring AI auto-configuration
                 * from requiring a real API key.
                 *
                 * TrackEmbeddingService itself is mocked
                 * below, so no OpenAI request is made.
                 */
                "spring.ai.openai.api-key=test-key"
        }
)
class TrackSearchProjectionIntegrationTests {

    private static final Duration ASYNC_TIMEOUT =
            Duration.ofSeconds(20);

    private static final int EMBEDDING_DIMENSIONS =
            1024;

    /*
     * PostgreSQL
     */
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

    /*
     * Kafka
     *
     * Same version already used by
     * PlaybackKafkaIntegrationTests.
     */
    @Container
    static final KafkaContainer KAFKA =
            new KafkaContainer(
                    DockerImageName.parse(
                            "apache/kafka:4.3.1"
                    )
            );

    /*
     * Redis
     *
     * TrackService update/delete use cache eviction,
     * so give the test a real Redis instance rather
     * than depending on localhost.
     */
    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(
                    DockerImageName.parse(
                            "redis:8-alpine"
                    )
            )
                    .withExposedPorts(
                            6379
                    )
                    .waitingFor(
                            Wait.forListeningPort()
                    );

    /*
     * OpenSearch
     *
     * Same image/version/configuration as compose.yml.
     */
    @Container
    static final GenericContainer<?> OPENSEARCH =
            new GenericContainer<>(
                    DockerImageName.parse(
                            "opensearchproject/opensearch:3.7.0"
                    )
            )
                    .withEnv(
                            "discovery.type",
                            "single-node"
                    )
                    .withEnv(
                            "DISABLE_SECURITY_PLUGIN",
                            "true"
                    )
                    .withEnv(
                            "OPENSEARCH_JAVA_OPTS",
                            "-Xms512m -Xmx512m"
                    )
                    .withExposedPorts(
                            9200
                    )
                    .waitingFor(
                            Wait.forHttp(
                                            "/_cluster/health"
                                    )
                                    .forPort(
                                            9200
                                    )
                                    .forStatusCode(
                                            200
                                    )
                    )
                    .withStartupTimeout(
                            Duration.ofMinutes(
                                    2
                            )
                    );

    /*
     * Feed container addresses into Spring.
     */
    @DynamicPropertySource
    static void infrastructureProperties(
            DynamicPropertyRegistry registry) {

        registry.add(
                "spring.kafka.bootstrap-servers",
                KAFKA::getBootstrapServers
        );

        registry.add(
                "app.opensearch.host",
                OPENSEARCH::getHost
        );

        registry.add(
                "app.opensearch.port",
                () ->
                        OPENSEARCH.getMappedPort(
                                9200
                        )
        );

        registry.add(
                "app.opensearch.scheme",
                () -> "http"
        );

        registry.add(
                "spring.data.redis.host",
                REDIS::getHost
        );

        registry.add(
                "spring.data.redis.port",
                () ->
                        REDIS.getMappedPort(
                                6379
                        )
        );
    }

    @Autowired
    private TrackService trackService;

    @Autowired
    private ArtistRepository artistRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private OpenSearchClient openSearchClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /*
     * This is the ONLY mocked part of the
     * distributed pipeline.
     *
     * We want to test:
     *
     * Postgres
     * -> outbox
     * -> Kafka
     * -> consumer
     * -> OpenSearch
     *
     * We do not want a network call to OpenAI.
     */
    @MockitoBean
    private TrackEmbeddingService trackEmbeddingService;

    @Test
    void createUpdateDeleteFlowsThroughOutboxKafkaAndOpenSearch()
            throws Exception {

        mockEmbedding();

        UUID albumId =
                createTestAlbum();

        /*
         * ------------------------------------------------
         * CREATE
         * ------------------------------------------------
         */
        TrackResponse created =
                trackService.create(
                        albumId,
                        new CreateTrackRequest(
                                "Projection Integration Test",
                                99,
                                120_000,
                                false
                        )
                );

        UUID trackId =
                created.id();

        /*
         * Wait until:
         *
         * TrackService
         * -> outbox
         * -> publisher
         * -> Kafka
         * -> projection consumer
         * -> OpenSearch
         */
        TrackSearchDocument createdDocument =
                awaitDocument(
                        trackId
                );

        assertThat(
                createdDocument.trackId()
        ).isEqualTo(
                trackId
        );

        assertThat(
                createdDocument.title()
        ).isEqualTo(
                "Projection Integration Test"
        );

        assertThat(
                createdDocument.albumTitle()
        ).isEqualTo(
                "Projection Test Album"
        );

        assertThat(
                createdDocument.artistName()
        ).isEqualTo(
                "Projection Test Artist"
        );

        awaitOutboxPublished(
                trackId,
                "track.search.upserted.v1",
                1
        );

        /*
         * ------------------------------------------------
         * UPDATE
         * ------------------------------------------------
         */
        trackService.update(
                trackId,
                new UpdateTrackRequest(
                        "Projection Integration Updated",
                        99,
                        125_000,
                        true
                )
        );

        TrackSearchDocument updatedDocument =
                awaitDocumentWithTitle(
                        trackId,
                        "Projection Integration Updated"
                );

        assertThat(
                updatedDocument.title()
        ).isEqualTo(
                "Projection Integration Updated"
        );

        assertThat(
                updatedDocument.durationMs()
        ).isEqualTo(
                125_000
        );

        assertThat(
                updatedDocument.explicit()
        ).isTrue();

        /*
         * We should now have:
         *
         * create upsert
         * update upsert
         */
        awaitOutboxPublished(
                trackId,
                "track.search.upserted.v1",
                2
        );

        /*
         * ------------------------------------------------
         * DELETE
         * ------------------------------------------------
         */
        trackService.delete(
                trackId
        );

        awaitDocumentDeleted(
                trackId
        );

        awaitOutboxPublished(
                trackId,
                "track.search.deleted.v1",
                1
        );

        /*
         * Finally verify the catalog DB row
         * is actually gone too.
         */
        Integer trackCount =
                jdbcTemplate.queryForObject(
                        """
                        SELECT COUNT(*)
                        FROM tracks
                        WHERE id = ?
                        """,
                        Integer.class,
                        trackId
                );

        assertThat(
                trackCount
        ).isZero();
    }

    /*
     * ----------------------------------------------------
     * Test fixture
     * ----------------------------------------------------
     *
     * Artist and Album are prerequisites.
     *
     * Create them directly through repositories so
     * unrelated catalog events are not part of this test.
     */
    private UUID createTestAlbum() {

        Artist artist =
                artistRepository.saveAndFlush(
                        new Artist(
                                "Projection Test Artist",
                                null,
                                null
                        )
                );

        Album album =
                albumRepository.saveAndFlush(
                        new Album(
                                artist,
                                "Projection Test Album",
                                LocalDate.of(
                                        2026,
                                        1,
                                        1
                                ),
                                null
                        )
                );

        return album.getId();
    }

    /*
     * Return a deterministic vector.
     *
     * TrackEmbeddingService currently expects
     * 1024 dimensions.
     */
    private void mockEmbedding() {

        List<Float> embedding =
                Collections.nCopies(
                        EMBEDDING_DIMENSIONS,
                        0.01F
                );

        when(
                trackEmbeddingService.embed(
                        anyString()
                )
        ).thenReturn(
                embedding
        );
    }

    /*
     * Wait for CREATE projection.
     */
    private TrackSearchDocument awaitDocument(
            UUID trackId)
            throws Exception {

        long deadline =
                System.nanoTime()
                        + ASYNC_TIMEOUT.toNanos();

        while (System.nanoTime()
                < deadline) {

            GetResponse<TrackSearchDocument> response =
                    getDocument(
                            trackId
                    );

            if (response != null
                    && response.found()
                    && response.source() != null) {

                return response.source();
            }

            Thread.sleep(
                    100
            );
        }

        throw new AssertionError(
                "Timed out waiting for OpenSearch document "
                        + trackId
        );
    }
    /*
     * Wait until UPDATE has reached OpenSearch.
     *
     * Merely checking that the document exists is
     * insufficient because the CREATE projection
     * may still be the document currently stored.
     */
    private TrackSearchDocument awaitDocumentWithTitle(
            UUID trackId,
            String expectedTitle)
            throws Exception {

        long deadline =
                System.nanoTime()
                        + ASYNC_TIMEOUT.toNanos();

        TrackSearchDocument latest =
                null;

        while (System.nanoTime()
                < deadline) {

            GetResponse<TrackSearchDocument> response =
                    getDocument(
                            trackId
                    );

            if (response.found()
                    && response.source()
                    != null) {

                latest =
                        response.source();

                if (expectedTitle.equals(
                        latest.title()
                )) {

                    return latest;
                }
            }

            Thread.sleep(
                    100
            );
        }

        throw new AssertionError(
                "Timed out waiting for updated "
                        + "OpenSearch document. "
                        + "trackId="
                        + trackId
                        + ", expectedTitle="
                        + expectedTitle
                        + ", latest="
                        + latest
        );
    }

    /*
     * Wait for DELETE projection.
     */
    private void awaitDocumentDeleted(
            UUID trackId)
            throws Exception {

        long deadline =
                System.nanoTime()
                        + ASYNC_TIMEOUT.toNanos();

        while (System.nanoTime()
                < deadline) {

            GetResponse<TrackSearchDocument> response =
                    getDocument(
                            trackId
                    );

            if (!response.found()) {
                return;
            }

            Thread.sleep(
                    100
            );
        }

        throw new AssertionError(
                "Timed out waiting for OpenSearch "
                        + "document deletion. trackId="
                        + trackId
        );
    }

     
    private GetResponse<TrackSearchDocument> getDocument(
            UUID trackId)
            throws IOException {

        try {

            return openSearchClient.get(
                    request ->
                            request
                                    .index(
                                            SearchIndexNames.TRACKS
                                    )
                                    .id(
                                            trackId.toString()
                                    ),
                    TrackSearchDocument.class
            );

        } catch (OpenSearchException exception) {

            if (isTransientOpenSearchReadFailure(
                    exception
            )) {
                return null;
            }

            throw exception;
        }
    }

    private boolean isTransientOpenSearchReadFailure(
            OpenSearchException exception) {

        String errorType =
                exception
                        .error()
                        .type();

        return switch (errorType) {

            /*
             * Consumer has not created the
             * search index yet.
             */
            case "index_not_found_exception" ->
                    true;

            /*
             * Index exists but its primary
             * shard is still starting/recovering.
             */
            case "no_shard_available_action_exception" ->
                    true;

            case "illegal_index_shard_state_exception" ->
                    true;

            case "unavailable_shards_exception" ->
                    true;

            default ->
                    false;
        };
    }

    /*
     * Verify outbox status as well.
     *
     * aggregate_id is the track UUID.
     */
    private void awaitOutboxPublished(
            UUID trackId,
            String eventType,
            int expectedCount)
            throws InterruptedException {

        long deadline =
                System.nanoTime()
                        + ASYNC_TIMEOUT.toNanos();

        int latestCount =
                0;

        while (System.nanoTime()
                < deadline) {

            Integer count =
                    jdbcTemplate.queryForObject(
                            """
                            SELECT COUNT(*)
                            FROM outbox_events
                            WHERE aggregate_id = ?
                              AND event_type = ?
                              AND status = 'PUBLISHED'
                            """,
                            Integer.class,
                            trackId,
                            eventType
                    );

            latestCount =
                    count == null
                            ? 0
                            : count;

            if (latestCount
                    == expectedCount) {

                return;
            }

            Thread.sleep(
                    100
            );
        }

        throw new AssertionError(
                "Timed out waiting for outbox events. "
                        + "trackId="
                        + trackId
                        + ", eventType="
                        + eventType
                        + ", expectedCount="
                        + expectedCount
                        + ", actualCount="
                        + latestCount
        );
    }
}