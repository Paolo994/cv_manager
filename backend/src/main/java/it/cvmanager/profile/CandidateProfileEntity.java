package it.cvmanager.profile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "candidate_profile")
class CandidateProfileEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "source_cv_id")
    private UUID sourceCvId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String profile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProfileStatus status;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    protected CandidateProfileEntity() {
        // JPA
    }

    CandidateProfileEntity(UUID userId) {
        this.userId = userId;
    }

    void replaceWithDraft(UUID sourceCvId, String profileJson) {
        this.sourceCvId = sourceCvId;
        this.profile = profileJson;
        this.status = ProfileStatus.DRAFT;
        this.updatedAt = Instant.now();
        this.confirmedAt = null;
    }

    void edit(String profileJson, boolean confirm) {
        this.profile = profileJson;
        this.updatedAt = Instant.now();
        this.status = confirm ? ProfileStatus.CONFIRMED : ProfileStatus.DRAFT;
        this.confirmedAt = confirm ? updatedAt : null;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getSourceCvId() {
        return sourceCvId;
    }

    String getProfile() {
        return profile;
    }

    ProfileStatus getStatus() {
        return status;
    }

    Instant getUpdatedAt() {
        return updatedAt;
    }

    Instant getConfirmedAt() {
        return confirmedAt;
    }
}
