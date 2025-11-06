package com.naitabdallah.aipitchdeck.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs for Project operations.
 */
public class ProjectDto {

    public record CreateProjectRequest(
            @NotBlank(message = "Project name is required")
            @Size(max = 255, message = "Project name must not exceed 255 characters")
            String name,

            String description,
            String industry,
            String targetAudience
    ) {}

    public record UpdateProjectRequest(
            String name,
            String description,
            String industry,
            String targetAudience,
            String status
    ) {}

    public record ProjectResponse(
            UUID id,
            UUID userId,
            String name,
            String description,
            String industry,
            String targetAudience,
            String status,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
