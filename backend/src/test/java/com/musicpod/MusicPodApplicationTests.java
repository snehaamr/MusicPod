package com.musicpod;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class MusicPodApplicationTests {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("musicpod")
                    .withUsername("musicpod")
                    .withPassword("musicpod");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void applicationStartsAndConnectsToPostgres() {

        Integer result = jdbcTemplate.queryForObject(
                "SELECT 1",
                Integer.class
        );

        assertEquals(1, result);
    }

    @Test
    void flywayBaselineMigrationWasApplied() {

        Long migrationCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE version = '1'
                  AND success = TRUE
                """,
                Long.class
        );

        assertEquals(1L, migrationCount);
    }

    @Test
    void postgresContainerIsRunning() {
        assertTrue(postgres.isRunning());
    }
}