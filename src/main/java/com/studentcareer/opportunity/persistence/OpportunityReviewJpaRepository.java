package com.studentcareer.opportunity.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityReviewJpaRepository extends JpaRepository<OpportunityReviewEntity, Long> {
    List<OpportunityReviewEntity> findByOpportunityIdOrderByIdAsc(UUID id);
}
