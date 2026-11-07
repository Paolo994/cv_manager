package it.cvmanager.jobs;

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
@Table(name = "job_posting")
class JobPosting {

    enum Source {
        MANUAL
    }

    @Id
    private UUID id;

    @Column(name = "owner_user_id")
    private UUID ownerUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Source source;

    @Column(name = "external_id")
    private String externalId;

    @Column(length = 1000)
    private String url;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    private String analysis;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected JobPosting() {
        // JPA
    }

    static JobPosting manual(UUID ownerUserId, String description, String url) {
        JobPosting posting = new JobPosting();
        posting.id = UUID.randomUUID();
        posting.ownerUserId = ownerUserId;
        posting.source = Source.MANUAL;
        posting.description = description;
        posting.url = url;
        posting.createdAt = Instant.now();
        return posting;
    }

    void setAnalysis(String analysisJson) {
        this.analysis = analysisJson;
    }

    UUID getId() {
        return id;
    }

    Source getSource() {
        return source;
    }

    String getUrl() {
        return url;
    }

    String getDescription() {
        return description;
    }

    String getAnalysis() {
        return analysis;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
