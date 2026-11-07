package it.cvmanager.cv.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cv_review")
class CvReviewEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "cv_id", nullable = false)
    private UUID cvId;

    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "job_description", columnDefinition = "text")
    private String jobDescription;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String review;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CvReviewEntity() {
        // JPA
    }

    CvReviewEntity(UUID userId, UUID cvId, UUID runId, String jobDescription, String reviewJson) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.cvId = cvId;
        this.runId = runId;
        this.jobDescription = jobDescription;
        this.review = reviewJson;
        this.createdAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    UUID getCvId() {
        return cvId;
    }

    UUID getRunId() {
        return runId;
    }

    String getJobDescription() {
        return jobDescription;
    }

    String getReview() {
        return review;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
