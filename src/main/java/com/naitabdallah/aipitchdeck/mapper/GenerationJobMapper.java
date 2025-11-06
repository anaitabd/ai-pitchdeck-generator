package com.naitabdallah.aipitchdeck.mapper;

import com.naitabdallah.aipitchdeck.dto.GenerationDto;
import com.naitabdallah.aipitchdeck.entity.GenerationJob;
import org.springframework.stereotype.Component;

/**
 * Mapper for GenerationJob entity and DTOs.
 */
@Component
public class GenerationJobMapper {

    public GenerationDto.GenerationJobResponse toResponse(GenerationJob job) {
        return new GenerationDto.GenerationJobResponse(
                job.getId(),
                job.getProjectId(),
                job.getStatus().name(),
                job.getAiModel(),
                job.getRetryCount(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt()
        );
    }
}
