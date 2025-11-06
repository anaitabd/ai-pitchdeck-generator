package com.naitabdallah.aipitchdeck.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTOs for AI Generation operations.
 */
public class GenerationDto {

    public record StartGenerationRequest(
            @NotNull(message = "Project ID is required")
            UUID projectId,

            @NotEmpty(message = "At least one file must be provided")
            List<UUID> fileIds,

            String promptTemplate
    ) {}

    public record GenerationJobResponse(
            UUID id,
            UUID projectId,
            String status,
            String aiModel,
            Integer retryCount,
            String errorMessage,
            Instant createdAt,
            Instant startedAt,
            Instant completedAt
    ) {}
}
