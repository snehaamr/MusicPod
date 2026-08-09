package com.musicpod.ai.audit;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunRepository
        extends JpaRepository<AgentRun, UUID> {

    Optional<AgentRun> findByIdAndUserId(
            UUID id,
            UUID userId);
}