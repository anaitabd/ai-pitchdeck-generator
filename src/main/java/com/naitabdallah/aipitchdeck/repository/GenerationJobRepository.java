package com.naitabdallah.aipitchdeck.repository;

import com.naitabdallah.aipitchdeck.entity.GenerationJob;
import com.naitabdallah.aipitchdeck.entity.GenerationJob.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for GenerationJob entity operations.
 */
@Repository
public interface GenerationJobRepository extends JpaRepository<GenerationJob, UUID> {
    
    List<GenerationJob> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
    
    List<GenerationJob> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    List<GenerationJob> findByStatus(JobStatus status);
    
    Optional<GenerationJob> findByIdAndUserId(UUID id, UUID userId);
    
    long countByUserIdAndStatus(UUID userId, JobStatus status);
}
