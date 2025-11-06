package com.naitabdallah.aipitchdeck.service;

import com.naitabdallah.aipitchdeck.dto.FileUploadDto;
import com.naitabdallah.aipitchdeck.entity.FileUpload;
import com.naitabdallah.aipitchdeck.entity.Project;
import com.naitabdallah.aipitchdeck.exception.FileUploadException;
import com.naitabdallah.aipitchdeck.exception.ResourceNotFoundException;
import com.naitabdallah.aipitchdeck.mapper.FileUploadMapper;
import com.naitabdallah.aipitchdeck.repository.FileUploadRepository;
import com.naitabdallah.aipitchdeck.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for file upload management.
 */
@Service
public class FileUploadService {

    private final FileUploadRepository fileUploadRepository;
    private final ProjectRepository projectRepository;
    private final S3Service s3Service;
    private final FileUploadMapper fileUploadMapper;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${application.file-upload.allowed-extensions}")
    private String allowedExtensions;

    public FileUploadService(FileUploadRepository fileUploadRepository,
                             ProjectRepository projectRepository,
                             S3Service s3Service,
                             FileUploadMapper fileUploadMapper) {
        this.fileUploadRepository = fileUploadRepository;
        this.projectRepository = projectRepository;
        this.s3Service = s3Service;
        this.fileUploadMapper = fileUploadMapper;
    }

    @Transactional
    public FileUploadDto.PresignedUrlResponse generatePresignedUrl(
            UUID userId,
            FileUploadDto.PresignedUrlRequest request) {

        // Validate project ownership
        Project project = projectRepository.findByIdAndUserId(request.projectId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.projectId()));

        // Validate file extension
        String fileExtension = getFileExtension(request.filename());
        validateFileExtension(fileExtension);

        // Generate presigned URL
        S3Service.PresignedUrlResponse presignedUrl = s3Service.generatePresignedUploadUrl(
                request.filename(),
                request.contentType(),
                userId,
                request.projectId()
        );

        // Create file upload record
        FileUpload fileUpload = FileUpload.builder()
                .projectId(request.projectId())
                .userId(userId)
                .originalFilename(request.filename())
                .storedFilename(request.filename())
                .fileType(FileUpload.FileType.valueOf(fileExtension.toUpperCase()))
                .fileSize(0L)  // Will be updated after upload
                .s3Key(presignedUrl.key())
                .s3Bucket(bucketName)
                .uploadStatus(FileUpload.UploadStatus.PENDING)
                .build();

        fileUpload = fileUploadRepository.save(fileUpload);

        return new FileUploadDto.PresignedUrlResponse(
                presignedUrl.url(),
                presignedUrl.key(),
                fileUpload.getId(),
                presignedUrl.expiresIn()
        );
    }

    @Transactional
    public FileUploadDto.FileUploadResponse confirmUpload(UUID uploadId, UUID userId, Long fileSize) {
        FileUpload fileUpload = fileUploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("FileUpload", "id", uploadId));

        if (!fileUpload.getUserId().equals(userId)) {
            throw new FileUploadException("Unauthorized access to file upload");
        }

        // Verify file exists in S3
        if (!s3Service.fileExists(fileUpload.getS3Key())) {
            fileUpload.setUploadStatus(FileUpload.UploadStatus.FAILED);
            fileUploadRepository.save(fileUpload);
            throw new FileUploadException("File not found in S3");
        }

        fileUpload.setFileSize(fileSize);
        fileUpload.setUploadStatus(FileUpload.UploadStatus.COMPLETED);
        fileUpload.setProcessedAt(Instant.now());

        fileUpload = fileUploadRepository.save(fileUpload);
        return fileUploadMapper.toResponse(fileUpload);
    }

    public List<FileUploadDto.FileUploadResponse> getProjectFiles(UUID projectId, UUID userId) {
        // Verify project ownership
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        return fileUploadRepository.findByProjectId(projectId)
                .stream()
                .map(fileUploadMapper::toResponse)
                .toList();
    }

    public FileUploadDto.FileUploadResponse getFileUpload(UUID uploadId, UUID userId) {
        FileUpload fileUpload = fileUploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("FileUpload", "id", uploadId));

        if (!fileUpload.getUserId().equals(userId)) {
            throw new FileUploadException("Unauthorized access to file upload");
        }

        return fileUploadMapper.toResponse(fileUpload);
    }

    @Transactional
    public void deleteFileUpload(UUID uploadId, UUID userId) {
        FileUpload fileUpload = fileUploadRepository.findById(uploadId)
                .orElseThrow(() -> new ResourceNotFoundException("FileUpload", "id", uploadId));

        if (!fileUpload.getUserId().equals(userId)) {
            throw new FileUploadException("Unauthorized access to file upload");
        }

        // Delete from S3
        s3Service.deleteFile(fileUpload.getS3Key());

        // Delete from database
        fileUploadRepository.delete(fileUpload);
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new FileUploadException("File must have an extension");
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    private void validateFileExtension(String extension) {
        List<String> allowed = Arrays.asList(allowedExtensions.split(","));
        if (!allowed.contains(extension)) {
            throw new FileUploadException("File type not allowed: " + extension + ". Allowed types: " + allowedExtensions);
        }
    }
}
