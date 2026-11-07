package it.cvmanager.profile.preferences;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_preferences")
class JobPreferencesEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String preferences;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected JobPreferencesEntity() {
        // JPA
    }

    JobPreferencesEntity(UUID userId) {
        this.userId = userId;
    }

    void update(String preferencesJson) {
        this.preferences = preferencesJson;
        this.updatedAt = Instant.now();
    }

    String getPreferences() {
        return preferences;
    }
}
