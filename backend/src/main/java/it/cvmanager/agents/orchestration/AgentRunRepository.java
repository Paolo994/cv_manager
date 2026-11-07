package it.cvmanager.agents.orchestration;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {

    Optional<AgentRun> findByIdAndUserId(UUID id, UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AgentRun r where r.id = ?1")
    Optional<AgentRun> findByIdForUpdate(UUID id);

    List<AgentRun> findByStatus(RunStatus status);
}
