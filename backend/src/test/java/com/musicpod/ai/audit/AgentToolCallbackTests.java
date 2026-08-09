package com.musicpod.ai.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

import com.musicpod.ai.curator.CuratorToolRisk;

@ExtendWith(MockitoExtension.class)
class AuditedToolCallbackTests {

    @Mock
    private ToolCallback delegate;

    @Mock
    private AgentStepService agentStepService;

    private ToolDefinition toolDefinition;

    @BeforeEach
    void setUp() {

        toolDefinition =
                ToolDefinition.builder()
                        .name(
                                "failingTool"
                        )
                        .description(
                                "A test tool"
                        )
                        .inputSchema(
                                """
                                {
                                  "type": "object"
                                }
                                """
                        )
                        .build();

        when(
                delegate.getToolDefinition()
        ).thenReturn(
                toolDefinition
        );
    }

    @Test
    void failedToolCallIsRecordedAsFailed() {

        UUID runId =
                UUID.randomUUID();

        UUID stepId =
                UUID.randomUUID();

        AgentExecutionContext executionContext =
                new AgentExecutionContext(
                        runId
                );

        ToolContext toolContext =
                new ToolContext(
                        Map.of(
                                AuditedToolCallback
                                        .AGENT_EXECUTION_CONTEXT_KEY,
                                executionContext
                        )
                );

        AuditedToolCallback callback =
                new AuditedToolCallback(
                        delegate,
                        CuratorToolRisk.READ_ONLY,
                        agentStepService
                );

        String input =
                """
                {"query":"test"}
                """;

        when(
                agentStepService.start(
                        runId,
                        1,
                        "failingTool",
                        "READ_ONLY",
                        input
                )
        ).thenReturn(
                stepId
        );

        IllegalStateException failure =
                new IllegalStateException(
                        "Tool exploded"
                );

        when(
                delegate.call(
                        input,
                        toolContext
                )
        ).thenThrow(
                failure
        );

        IllegalStateException thrown =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                callback.call(
                                        input,
                                        toolContext
                                )
                );

        assertEquals(
                "Tool exploded",
                thrown.getMessage()
        );

        verify(
                agentStepService
        ).start(
                runId,
                1,
                "failingTool",
                "READ_ONLY",
                input
        );

        verify(
                agentStepService
        ).fail(
                same(stepId),
                same(failure),
                anyLong()
        );
    }
}