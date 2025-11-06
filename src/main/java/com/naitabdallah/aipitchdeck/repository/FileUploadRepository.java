package com.naitabdallah.aipitchdeck.repository;

import com.naitabdallah.aipitchdeck.entity.FileUpload;
import com.naitabdallah.aipitchdeck.entity.FileUpload.UploadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for FileUpload entity operations.
 */
@Repository
public interface FileUploadRepository extends JpaRepository<FileUpload, UUID> {
    
    List<FileUpload> findByProjectId(UUID projectId);
    
    List<FileUpload> findByUserIdAndUploadStatus(UUID userId, UploadStatus status);
    
    Optional<FileUpload> findByS3Key(String s3Key);
    
    List<FileUpload> findByProjectIdAndUploadStatus(UUID projectId, UploadStatus status);
}
