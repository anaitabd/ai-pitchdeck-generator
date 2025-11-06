package com.naitabdallah.aipitchdeck.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * Service for AWS S3 operations.
 */
@Service
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.presigned-url-expiration-minutes}")
    private int presignedUrlExpirationMinutes;

    public S3Service(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    /**
     * Generate a presigned URL for uploading a file to S3.
     */
    public PresignedUrlResponse generatePresignedUploadUrl(String fileName, String contentType, UUID userId, UUID projectId) {
        String key = generateS3Key(userId, projectId, fileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return new PresignedUrlResponse(
                presignedRequest.url().toString(),
                key,
                presignedUrlExpirationMinutes * 60
        );
    }

    /**
     * Upload a file directly to S3.
     */
    public String uploadFile(byte[] fileContent, String fileName, String contentType, UUID userId, UUID projectId) {
        String key = generateS3Key(userId, projectId, fileName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileContent));

        return key;
    }

    /**
     * Delete a file from S3.
     */
    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    /**
     * Check if a file exists in S3.
     */
    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * Generate S3 key for a file.
     */
    private String generateS3Key(UUID userId, UUID projectId, String fileName) {
        String sanitizedFileName = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        return String.format("uploads/%s/%s/%s_%s",
                userId,
                projectId,
                UUID.randomUUID(),
                sanitizedFileName
        );
    }

    public record PresignedUrlResponse(String url, String key, long expiresIn) {}
}
