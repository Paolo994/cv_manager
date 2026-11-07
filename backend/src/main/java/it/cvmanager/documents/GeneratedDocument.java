package it.cvmanager.documents;

import it.cvmanager.shared.Language;
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
@Table(name = "generated_document")
class GeneratedDocument {

    enum Origin {
        /** Generato dagli agenti. */
        AI,
        /** Modificato dall'utente. */
        USER
    }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType type;

    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Origin origin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Language language;

    @Column(name = "run_id")
    private UUID runId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "review_notes")
    private String reviewNotes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GeneratedDocument() {
        // JPA
    }

    GeneratedDocument(UUID userId, UUID jobId, DocumentType type, int version, Origin origin, Language language,
                      UUID runId, String contentJson, String reviewNotesJson) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.jobId = jobId;
        this.type = type;
        this.version = version;
        this.origin = origin;
        this.language = language;
        this.runId = runId;
        this.content = contentJson;
        this.reviewNotes = reviewNotesJson;
        this.createdAt = Instant.now();
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getJobId() {
        return jobId;
    }

    DocumentType getType() {
        return type;
    }

    int getVersion() {
        return version;
    }

    Origin getOrigin() {
        return origin;
    }

    Language getLanguage() {
        return language;
    }

    UUID getRunId() {
        return runId;
    }

    String getContent() {
        return content;
    }

    String getReviewNotes() {
        return reviewNotes;
    }

    Instant getCreatedAt() {
        return createdAt;
    }
}
