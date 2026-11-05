package com.studentcareer.company.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import java.net.URI;
import com.studentcareer.source.model.SourceReference;
import com.studentcareer.source.model.SourceKind;

@Entity
@Table(name = "source_references")
public class SourceReferenceEntity {
    @Id
    UUID id;
    @Column(name = "source_url", nullable = false, columnDefinition = "text")
    String url;
    @Column(nullable = false, columnDefinition = "text")
    String title;
    @Column(name = "source_kind", nullable = false, length = 32)
    String kind;
    @Column(name = "accessed_at", nullable = false)
    Instant accessedAt;
    @Column(name = "source_updated_at")
    Instant sourceUpdatedAt;
    @Column(length = 128)
    String license;
    @Column(name = "verification_status", nullable = false, length = 32)
    String verificationStatus;
    @Column(name = "verified_at")
    Instant verifiedAt;

    protected SourceReferenceEntity() { }
    public void updateFrom(UUID sourceId, SourceReference source) {
        id = sourceId;
        url = source.getUrl().toString();
        title = source.getTitle();
        kind = source.getKind().name();
        accessedAt = source.getAccessedAt();
        sourceUpdatedAt = source.getSourceUpdatedAt();
        license = source.getLicense();
        verificationStatus = source.getVerificationStatus().name();
        verifiedAt = null;
    }

    public SourceReference toDomain() {
        SourceReference source = new SourceReference(URI.create(url), title, accessedAt,
                SourceKind.valueOf(kind), sourceUpdatedAt, license);
        if (!source.getVerificationStatus().name().equals(verificationStatus) || verifiedAt != null) {
            throw new IllegalStateException("Unsupported persisted verification state");
        }
        return source;
    }
}
