package it.cvmanager.cv.review;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CvReviewRepository extends JpaRepository<CvReviewEntity, UUID> {

    List<CvReviewEntity> findByCvIdAndUserIdOrderByCreatedAtDesc(UUID cvId, UUID userId);
}
