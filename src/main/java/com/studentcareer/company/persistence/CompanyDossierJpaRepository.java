package com.studentcareer.company.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyDossierJpaRepository extends JpaRepository<CompanyDossierEntity, UUID> {
    @EntityGraph(attributePaths = "source")
    List<CompanyDossierEntity> findAllByOrderByImportOrderAsc();
}
