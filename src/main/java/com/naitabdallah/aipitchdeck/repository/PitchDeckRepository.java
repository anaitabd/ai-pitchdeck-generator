package com.naitabdallah.aipitchdeck.repository;

import com.naitabdallah.aipitchdeck.entity.PitchDeck;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for PitchDeck entity operations.
 */
@Repository
public interface PitchDeckRepository extends JpaRepository<PitchDeck, UUID> {
    
    List<PitchDeck> findByProjectIdOrderByVersionDesc(UUID projectId);
    
    Optional<PitchDeck> findByProjectIdAndIsCurrentVersion(UUID projectId, boolean isCurrentVersion);
    
    Optional<PitchDeck> findByIdAndUserId(UUID id, UUID userId);
    
    @Modifying
    @Query("UPDATE PitchDeck p SET p.isCurrentVersion = false WHERE p.projectId = :projectId")
    void unsetCurrentVersionForProject(@Param("projectId") UUID projectId);
    
    long countByProjectId(UUID projectId);
}
