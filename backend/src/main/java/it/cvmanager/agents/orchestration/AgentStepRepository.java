package it.cvmanager.agents.orchestration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface AgentStepRepository extends JpaRepository<AgentStep, UUID> {

    List<AgentStep> findByRunIdOrderByStartedAt(UUID runId);
}
