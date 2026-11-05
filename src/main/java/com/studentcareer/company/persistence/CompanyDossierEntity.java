package com.studentcareer.company.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "company_dossiers")
public class CompanyDossierEntity {
    @Id
    UUID id;
    @Column(nullable = false, length = 512)
    String name;
    @Column(columnDefinition = "text")
    String website;
    @Column(columnDefinition = "text")
    String industry;
    @Column(columnDefinition = "text")
    String location;
    @Column(name = "import_status", nullable = false, length = 32)
    String importStatus;
    @Column(name = "duplicate_of", columnDefinition = "text")
    String duplicateOf;
    @Column(name = "import_order", insertable = false, updatable = false)
    Long importOrder;
    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL)
    @JoinColumn(name = "source_id", nullable = false, unique = true)
    SourceReferenceEntity source;

    protected CompanyDossierEntity() { }
}
