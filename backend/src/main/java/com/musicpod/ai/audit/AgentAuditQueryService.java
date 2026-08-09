package com.musicpod.ai.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.musicpod.common.exception.ResourceNotFoundException;

@Service
public class AgentAuditQueryService {

    private final AgentRunRepository agentRunRepository;
    private final AgentStepRepository agentStepRepository;

    public AgentAuditQueryService(
            AgentRunRepository agentRunRepository,
            AgentStepRepository agentStepRepository) {

        this.agentRunRepository = agentRunRepository;
        this.agentStepRepository = agentStepRepository;
    }

    @Transactional(readOnly = true)
    public AgentRunResponse getRun(
            UUID userId,
            UUID runId) {

        AgentRun run =
                agentRunRepository
                        .findByIdAndUserId(
                                runId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Agent run not found: "
                                                + runId
                                )
                        );

        List<AgentStepResponse> steps =
                agentStepRepository
                        .findByRunIdOrderBySequenceNumberAsc(
                                runId
                        )
                        .stream()
                        .map(AgentStepResponse::from)
                        .toList();

        return new AgentRunResponse(
                run.getId(),
                run.getPrompt(),
                run.isAllowWrite(),
                run.getStatus(),
                run.getResponse(),
                run.getErrorMessage(),
                run.getStartedAt(),
                run.getCompletedAt(),
                steps
        );
    }
}