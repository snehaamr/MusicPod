package com.musicpod.ai.audit;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentStepRepository
        extends JpaRepository<AgentStep, UUID> {

    List<AgentStep>
            findByRunIdOrderBySequenceNumberAsc(
                    UUID runId);
}