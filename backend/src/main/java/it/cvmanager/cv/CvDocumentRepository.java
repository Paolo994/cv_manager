package it.cvmanager.cv;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Ogni query filtra per proprietario: un utente non vede mai i CV di un altro. */
interface CvDocumentRepository extends JpaRepository<CvDocument, UUID> {

    List<CvDocument> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<CvDocument> findByIdAndUserId(UUID id, UUID userId);
}
