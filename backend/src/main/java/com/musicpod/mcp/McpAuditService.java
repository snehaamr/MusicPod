package com.musicpod.mcp;

import java.util.UUID;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.musicpod.ai.audit.AgentRunService;
import com.musicpod.ai.audit.AgentStepService;
import com.musicpod.auth.CurrentUserProvider;

import tools.jackson.databind.json.JsonMapper;

@Service
public class McpAuditService {

    public static final String RISK_READ_ONLY =
            "READ_ONLY";

    public static final String RISK_WRITE =
            "WRITE";

    private static final Logger log =
            LoggerFactory.getLogger(
                    McpAuditService.class
            );

    private static final int
            MCP_STEP_SEQUENCE = 1;

    private final CurrentUserProvider
            currentUserProvider;

    private final AgentRunService
            agentRunService;

    private final AgentStepService
            agentStepService;

    private final JsonMapper
            jsonMapper;

    public McpAuditService(
            CurrentUserProvider currentUserProvider,
            AgentRunService agentRunService,
            AgentStepService agentStepService,
            JsonMapper jsonMapper) {

        this.currentUserProvider =
                currentUserProvider;

        this.agentRunService =
                agentRunService;

        this.agentStepService =
                agentStepService;

        this.jsonMapper =
                jsonMapper;
    }

    /*
     * Normal MCP execution.
     *
     * The returned Java object is serialized to JSON
     * before being written to the audit tables.
     *
     * Use this for:
     *
     * - MCP tools returning DTOs
     * - lists
     * - PageResponse
     * - records
     */
    public <T> T execute(
            String operationName,
            boolean allowWrite,
            String risk,
            Object auditInput,
            Function<UUID, T> operation) {

        return executeInternal(
                operationName,
                allowWrite,
                risk,
                auditInput,
                operation,
                this::toJson
        );
    }

    /*
     * MCP resource execution.
     *
     * MusicPod MCP resources already return JSON
     * as a String.
     *
     * We therefore do NOT serialize the returned
     * String again for auditing.
     *
     * Without this method:
     *
     * resource returns:
     *
     *   {"id":"123","name":"Playlist"}
     *
     * audit output becomes:
     *
     *   "{\"id\":\"123\",\"name\":\"Playlist\"}"
     *
     * With executeJson(), the audit output remains:
     *
     *   {"id":"123","name":"Playlist"}
     */
    public String executeJson(
            String operationName,
            boolean allowWrite,
            String risk,
            Object auditInput,
            Function<UUID, String> operation) {

        return executeInternal(
                operationName,
                allowWrite,
                risk,
                auditInput,
                operation,
                value -> value
        );
    }

    private <T> T executeInternal(
            String operationName,
            boolean allowWrite,
            String risk,
            Object auditInput,
            Function<UUID, T> operation,
            Function<T, String> auditOutputSerializer) {

        UUID userId =
                currentUserProvider.userId();

        UUID runId =
                agentRunService.start(
                        userId,
                        "MCP: " + operationName,
                        allowWrite
                );

        UUID stepId;

        try {

            stepId =
                    agentStepService.start(
                            runId,
                            MCP_STEP_SEQUENCE,
                            operationName,
                            risk,
                            toJson(
                                    auditInput
                            )
                    );

        } catch (RuntimeException exception) {

            safeFailRun(
                    runId,
                    exception
            );

            throw exception;
        }

        long startedNanos =
                System.nanoTime();

        try {

            T result =
                    operation.apply(
                            userId
                    );

            long durationMs =
                    elapsedMillis(
                            startedNanos
                    );

            String outputJson =
                    safeSerializeOutput(
                            runId,
                            result,
                            auditOutputSerializer
                    );

            safeComplete(
                    runId,
                    stepId,
                    outputJson,
                    durationMs
            );

            return result;

        } catch (RuntimeException | Error exception) {

            long durationMs =
                    elapsedMillis(
                            startedNanos
                    );

            safeFailStep(
                    stepId,
                    exception,
                    durationMs
            );

            safeFailRun(
                    runId,
                    exception
            );

            throw exception;
        }
    }

    /*
     * Audit finalization must never make a
     * successful business operation appear failed.
     *
     * This is especially important for write tools
     * such as create playlist, where retrying could
     * create a duplicate.
     */
    private <T> String safeSerializeOutput(
            UUID runId,
            T result,
            Function<T, String> auditOutputSerializer) {

        try {

            return auditOutputSerializer.apply(
                    result
            );

        } catch (RuntimeException exception) {

            log.warn(
                    "Unable to serialize MCP audit output for run {}",
                    runId,
                    exception
            );

            return """
                    {"auditOutput":"unavailable"}
                    """;
        }
    }

    private void safeComplete(
            UUID runId,
            UUID stepId,
            String outputJson,
            long durationMs) {

        try {

            agentStepService.complete(
                    stepId,
                    outputJson,
                    durationMs
            );

        } catch (RuntimeException exception) {

            log.error(
                    "Unable to complete MCP audit step {}",
                    stepId,
                    exception
            );
        }

        try {

            agentRunService.complete(
                    runId,
                    outputJson
            );

        } catch (RuntimeException exception) {

            log.error(
                    "Unable to complete MCP audit run {}",
                    runId,
                    exception
            );
        }
    }

    private void safeFailStep(
            UUID stepId,
            Throwable throwable,
            long durationMs) {

        try {

            agentStepService.fail(
                    stepId,
                    throwable,
                    durationMs
            );

        } catch (RuntimeException exception) {

            log.error(
                    "Unable to record MCP audit step failure {}",
                    stepId,
                    exception
            );
        }
    }

    private void safeFailRun(
            UUID runId,
            Throwable throwable) {

        try {

            agentRunService.fail(
                    runId,
                    throwable
            );

        } catch (RuntimeException exception) {

            log.error(
                    "Unable to record MCP audit run failure {}",
                    runId,
                    exception
            );
        }
    }

    private String toJson(
            Object value) {

        if (value == null) {
            return null;
        }

        return jsonMapper
                .writeValueAsString(
                        value
                );
    }

    private long elapsedMillis(
            long startedNanos) {

        return (
                System.nanoTime()
                        - startedNanos
        ) / 1_000_000;
    }
}