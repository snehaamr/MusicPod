package com.musicpod.ai.audit;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.common.exception.ResourceNotFoundException;

@Service
public class AgentStepService {

    private final AgentStepRepository
            agentStepRepository;

    private final AuditPayloadSanitizer
            sanitizer;

    public AgentStepService(
            AgentStepRepository agentStepRepository,
            AuditPayloadSanitizer sanitizer) {

        this.agentStepRepository =
                agentStepRepository;

        this.sanitizer =
                sanitizer;
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public UUID start(
            UUID runId,
            int sequenceNumber,
            String toolName,
            String toolRisk,
            String inputJson) {

        AgentStep step =
                new AgentStep(
                        runId,
                        sequenceNumber,
                        toolName,
                        toolRisk,
                        sanitizer.toolPayload(
                                inputJson
                        )
                );

        AgentStep savedStep =
                agentStepRepository
                        .saveAndFlush(step);

        return savedStep.getId();
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void complete(
            UUID stepId,
            String outputJson,
            long durationMs) {

        AgentStep step =
                findStep(stepId);

        step.complete(
                sanitizer.toolPayload(
                        outputJson
                ),
                durationMs
        );
    }

    @Transactional(
            propagation = Propagation.REQUIRES_NEW
    )
    public void fail(
            UUID stepId,
            Throwable throwable,
            long durationMs) {

        AgentStep step =
                findStep(stepId);

        step.fail(
                sanitizer.error(
                        errorMessage(
                                throwable
                        )
                ),
                durationMs
        );
    }

    private AgentStep findStep(
            UUID stepId) {

        return agentStepRepository
                .findById(stepId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Agent step not found: "
                                        + stepId
                        )
                );
    }

    private String errorMessage(
            Throwable throwable) {

        if (throwable == null) {
            return "Unknown tool execution error";
        }

        if (throwable.getMessage() != null
                && !throwable
                        .getMessage()
                        .isBlank()) {

            return throwable.getMessage();
        }

        return throwable
                .getClass()
                .getSimpleName();
    }
}