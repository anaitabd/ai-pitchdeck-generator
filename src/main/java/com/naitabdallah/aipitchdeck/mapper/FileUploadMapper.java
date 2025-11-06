package com.naitabdallah.aipitchdeck.mapper;

import com.naitabdallah.aipitchdeck.dto.FileUploadDto;
import com.naitabdallah.aipitchdeck.entity.FileUpload;
import org.springframework.stereotype.Component;

/**
 * Mapper for FileUpload entity and DTOs.
 */
@Component
public class FileUploadMapper {

    public FileUploadDto.FileUploadResponse toResponse(FileUpload fileUpload) {
        return new FileUploadDto.FileUploadResponse(
                fileUpload.getId(),
                fileUpload.getProjectId(),
                fileUpload.getOriginalFilename(),
                fileUpload.getFileType().name(),
                fileUpload.getFileSize(),
                fileUpload.getUploadStatus().name(),
                fileUpload.getS3Key(),
                fileUpload.getCreatedAt()
        );
    }
}
