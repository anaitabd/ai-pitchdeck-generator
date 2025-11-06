package com.naitabdallah.aipitchdeck.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs for File Upload operations.
 */
public class FileUploadDto {

    public record FileUploadResponse(
            UUID id,
            UUID projectId,
            String originalFilename,
            String fileType,
            Long fileSize,
            String uploadStatus,
            String s3Key,
            Instant createdAt
    ) {}

    public record PresignedUrlRequest(
            UUID projectId,
            String filename,
            String contentType
    ) {}

    public record PresignedUrlResponse(
            String uploadUrl,
            String fileKey,
            UUID uploadId,
            long expiresIn
    ) {}
}
