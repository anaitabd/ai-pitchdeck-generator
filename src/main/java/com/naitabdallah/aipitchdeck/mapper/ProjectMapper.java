package com.naitabdallah.aipitchdeck.mapper;

import com.naitabdallah.aipitchdeck.dto.ProjectDto;
import com.naitabdallah.aipitchdeck.entity.Project;
import org.springframework.stereotype.Component;

/**
 * Mapper for Project entity and DTOs.
 */
@Component
public class ProjectMapper {

    public ProjectDto.ProjectResponse toResponse(Project project) {
        return new ProjectDto.ProjectResponse(
                project.getId(),
                project.getUserId(),
                project.getName(),
                project.getDescription(),
                project.getIndustry(),
                project.getTargetAudience(),
                project.getStatus().name(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }

    public Project toEntity(ProjectDto.CreateProjectRequest request, java.util.UUID userId) {
        return Project.builder()
                .userId(userId)
                .name(request.name())
                .description(request.description())
                .industry(request.industry())
                .targetAudience(request.targetAudience())
                .status(Project.ProjectStatus.DRAFT)
                .build();
    }
}
