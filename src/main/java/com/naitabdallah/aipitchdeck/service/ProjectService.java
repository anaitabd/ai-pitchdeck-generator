package com.naitabdallah.aipitchdeck.service;

import com.naitabdallah.aipitchdeck.dto.ProjectDto;
import com.naitabdallah.aipitchdeck.entity.Project;
import com.naitabdallah.aipitchdeck.exception.ResourceNotFoundException;
import com.naitabdallah.aipitchdeck.mapper.ProjectMapper;
import com.naitabdallah.aipitchdeck.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for project management operations.
 */
@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    public ProjectService(ProjectRepository projectRepository, ProjectMapper projectMapper) {
        this.projectRepository = projectRepository;
        this.projectMapper = projectMapper;
    }

    @Transactional
    public ProjectDto.ProjectResponse createProject(UUID userId, ProjectDto.CreateProjectRequest request) {
        Project project = projectMapper.toEntity(request, userId);
        project = projectRepository.save(project);
        return projectMapper.toResponse(project);
    }

    public ProjectDto.ProjectResponse getProject(UUID projectId, UUID userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        return projectMapper.toResponse(project);
    }

    public Page<ProjectDto.ProjectResponse> getUserProjects(UUID userId, Pageable pageable) {
        return projectRepository.findByUserId(userId, pageable)
                .map(projectMapper::toResponse);
    }

    @Transactional
    public ProjectDto.ProjectResponse updateProject(UUID projectId, UUID userId, ProjectDto.UpdateProjectRequest request) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        if (request.name() != null) {
            project.setName(request.name());
        }
        if (request.description() != null) {
            project.setDescription(request.description());
        }
        if (request.industry() != null) {
            project.setIndustry(request.industry());
        }
        if (request.targetAudience() != null) {
            project.setTargetAudience(request.targetAudience());
        }
        if (request.status() != null) {
            project.setStatus(Project.ProjectStatus.valueOf(request.status()));
        }

        project = projectRepository.save(project);
        return projectMapper.toResponse(project);
    }

    @Transactional
    public void deleteProject(UUID projectId, UUID userId) {
        Project project = projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        projectRepository.delete(project);
    }
}
