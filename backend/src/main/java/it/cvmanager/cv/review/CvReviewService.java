package it.cvmanager.cv.review;

import it.cvmanager.cv.CvService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CvReviewService {

    private final CvReviewRepository reviews;
    private final CvService cvService;
    private final JsonMapper jsonMapper;

    CvReviewService(CvReviewRepository reviews, CvService cvService, JsonMapper jsonMapper) {
        this.reviews = reviews;
        this.cvService = cvService;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public UUID save(UUID userId, UUID cvId, UUID runId, String jobDescription, CvReview review) {
        CvReviewEntity entity = new CvReviewEntity(userId, cvId, runId, jobDescription, jsonMapper.writeValueAsString(review));
        return reviews.save(entity).getId();
    }

    @Transactional(readOnly = true)
    public List<ReviewView> list(UUID userId, UUID cvId) {
        cvService.get(userId, cvId); // 404 se il CV non è dell'utente
        return reviews.findByCvIdAndUserIdOrderByCreatedAtDesc(cvId, userId).stream()
                .map(e -> new ReviewView(e.getId(), e.getCvId(), e.getRunId(), e.getJobDescription(),
                        jsonMapper.readValue(e.getReview(), CvReview.class), e.getCreatedAt()))
                .toList();
    }

    public record ReviewView(UUID id, UUID cvId, UUID runId, String jobDescription, CvReview review, Instant createdAt) {
    }
}
