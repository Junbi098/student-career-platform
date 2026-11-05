package com.studentcareer.opportunity.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityJpaRepository extends JpaRepository<OpportunityEntity, UUID> {
    Optional<OpportunityEntity> findByDuplicateKey(String key);
    List<OpportunityEntity> findAllByOrderByFirstSubmittedAtDescIdAsc();
}
