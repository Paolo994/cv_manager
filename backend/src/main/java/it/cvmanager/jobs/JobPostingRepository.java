package it.cvmanager.jobs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

    List<JobPosting> findByOwnerUserIdOrderByCreatedAtDesc(UUID ownerUserId);

    Optional<JobPosting> findByIdAndOwnerUserId(UUID id, UUID ownerUserId);
}
