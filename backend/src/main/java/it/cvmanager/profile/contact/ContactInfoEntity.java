package it.cvmanager.profile.contact;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contact_info")
class ContactInfoEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String contact;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ContactInfoEntity() {
        // JPA
    }

    ContactInfoEntity(UUID userId) {
        this.userId = userId;
    }

    void update(String contactJson) {
        this.contact = contactJson;
        this.updatedAt = Instant.now();
    }

    String getContact() {
        return contact;
    }
}
