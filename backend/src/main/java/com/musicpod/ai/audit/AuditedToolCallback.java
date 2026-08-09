package com.musicpod.ai.audit;

import java.util.UUID;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

import com.musicpod.ai.curator.CuratorToolRisk;

public class AuditedToolCallback
        implements ToolCallback {

    public static final String
            AGENT_EXECUTION_CONTEXT_KEY =
            "musicpodAgentExecutionContext";

    private final ToolCallback delegate;

    private final CuratorToolRisk risk;

    private final AgentStepService agentStepService;

    public AuditedToolCallback(
            ToolCallback delegate,
            CuratorToolRisk risk,
            AgentStepService agentStepService) {

        this.delegate =
                delegate;

        this.risk =
                risk;

        this.agentStepService =
                agentStepService;
    }

    @Override
    public ToolDefinition getToolDefinition() {

        return delegate
                .getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {

        return delegate
                .getToolMetadata();
    }

    @Override
    public String call(
            String toolInput) {

        throw new IllegalStateException(
                "MusicPod agent execution context is required"
        );
    }

    @Override
    public String call(
            String toolInput,
            ToolContext toolContext) {

        AgentExecutionContext executionContext =
                getExecutionContext(
                        toolContext
                );

        int sequenceNumber =
                executionContext
                        .nextSequence();

        String toolName =
                getToolDefinition()
                        .name();

        UUID stepId =
                agentStepService.start(
                        executionContext.runId(),
                        sequenceNumber,
                        toolName,
                        risk.name(),
                        toolInput
                );

        long startedNanos =
                System.nanoTime();

        try {

            String result =
                    delegate.call(
                            toolInput,
                            toolContext
                    );

            long durationMs =
                    elapsedMillis(
                            startedNanos
                    );

            agentStepService.complete(
                    stepId,
                    result,
                    durationMs
            );

            return result;

        } catch (RuntimeException | Error exception) {

            long durationMs =
                    elapsedMillis(
                            startedNanos
                    );

            agentStepService.fail(
                    stepId,
                    exception,
                    durationMs
            );

            throw exception;
        }
    }

    private AgentExecutionContext
            getExecutionContext(
                    ToolContext toolContext) {

        if (toolContext == null) {

            throw new IllegalStateException(
                    "ToolContext is required for audited MusicPod tools"
            );
        }

        Object value =
                toolContext
                        .getContext()
                        .get(
                                AGENT_EXECUTION_CONTEXT_KEY
                        );

        if (!(value
                instanceof AgentExecutionContext context)) {

            throw new IllegalStateException(
                    "MusicPod agent execution context is missing"
            );
        }

        return context;
    }

    private long elapsedMillis(
            long startedNanos) {

        return (
                System.nanoTime()
                        - startedNanos
        ) / 1_000_000;
    }
}