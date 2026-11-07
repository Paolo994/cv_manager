package it.cvmanager.agents.orchestration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {

    Optional<AgentRun> findByIdAndUserId(UUID id, UUID userId);

    List<AgentRun> findByStatus(RunStatus status);
}
