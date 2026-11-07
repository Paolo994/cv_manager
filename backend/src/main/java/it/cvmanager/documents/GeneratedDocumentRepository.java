package it.cvmanager.documents;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface GeneratedDocumentRepository extends JpaRepository<GeneratedDocument, UUID> {

    List<GeneratedDocument> findByUserIdAndJobIdOrderByTypeAscVersionDesc(UUID userId, UUID jobId);

    Optional<GeneratedDocument> findByIdAndUserId(UUID id, UUID userId);

    @Query("select coalesce(max(d.version), 0) from GeneratedDocument d where d.userId = ?1 and d.jobId = ?2 and d.type = ?3")
    int maxVersion(UUID userId, UUID jobId, DocumentType type);
}
