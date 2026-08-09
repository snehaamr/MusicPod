package com.musicpod.ai.audit;

import java.util.Arrays;
import java.util.List;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;

import com.musicpod.ai.curator.CuratorToolRisk;

@Component
public class AuditedToolsFactory {

    private final AgentStepService agentStepService;

    public AuditedToolsFactory(
            AgentStepService agentStepService) {

        this.agentStepService =
                agentStepService;
    }

    public List<ToolCallback> create(
            Object toolObject,
            CuratorToolRisk risk) {

        ToolCallback[] callbacks =
                ToolCallbacks.from(
                        toolObject
                );

        return Arrays.stream(
                        callbacks
                )
                .map(callback ->
                        new AuditedToolCallback(
                                callback,
                                risk,
                                agentStepService
                        )
                )
                .map(ToolCallback.class::cast)
                .toList();
    }
}