package com.naitabdallah.aipitchdeck.controller;

import com.naitabdallah.aipitchdeck.dto.FileUploadDto;
import com.naitabdallah.aipitchdeck.security.UserPrincipal;
import com.naitabdallah.aipitchdeck.service.FileUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for file upload operations.
 */
@RestController
@RequestMapping("/api/v1/uploads")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PostMapping("/presigned-url")
    public ResponseEntity<FileUploadDto.PresignedUrlResponse> generatePresignedUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody FileUploadDto.PresignedUrlRequest request) {
        FileUploadDto.PresignedUrlResponse response = fileUploadService.generatePresignedUrl(principal.userId(), request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{uploadId}/confirm")
    public ResponseEntity<FileUploadDto.FileUploadResponse> confirmUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID uploadId,
            @RequestParam Long fileSize) {
        FileUploadDto.FileUploadResponse response = fileUploadService.confirmUpload(uploadId, principal.userId(), fileSize);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<FileUploadDto.FileUploadResponse>> getProjectFiles(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID projectId) {
        List<FileUploadDto.FileUploadResponse> response = fileUploadService.getProjectFiles(projectId, principal.userId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{uploadId}")
    public ResponseEntity<FileUploadDto.FileUploadResponse> getFileUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID uploadId) {
        FileUploadDto.FileUploadResponse response = fileUploadService.getFileUpload(uploadId, principal.userId());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{uploadId}")
    public ResponseEntity<Void> deleteFileUpload(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID uploadId) {
        fileUploadService.deleteFileUpload(uploadId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
