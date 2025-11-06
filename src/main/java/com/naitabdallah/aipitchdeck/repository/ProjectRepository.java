package com.naitabdallah.aipitchdeck.repository;

import com.naitabdallah.aipitchdeck.entity.Project;
import com.naitabdallah.aipitchdeck.entity.Project.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Project entity operations.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    
    Page<Project> findByUserId(UUID userId, Pageable pageable);
    
    List<Project> findByUserIdAndStatus(UUID userId, ProjectStatus status);
    
    Optional<Project> findByIdAndUserId(UUID id, UUID userId);
    
    long countByUserId(UUID userId);
}
