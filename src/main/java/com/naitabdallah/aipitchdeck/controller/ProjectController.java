package com.naitabdallah.aipitchdeck.controller;

import com.naitabdallah.aipitchdeck.dto.ProjectDto;
import com.naitabdallah.aipitchdeck.security.UserPrincipal;
import com.naitabdallah.aipitchdeck.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for project management.
 */
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectDto.ProjectResponse> createProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProjectDto.CreateProjectRequest request) {
        ProjectDto.ProjectResponse response = projectService.createProject(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<ProjectDto.ProjectResponse>> getUserProjects(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ProjectDto.ProjectResponse> response = projectService.getUserProjects(principal.userId(), pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectDto.ProjectResponse> getProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID projectId) {
        ProjectDto.ProjectResponse response = projectService.getProject(projectId, principal.userId());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{projectId}")
    public ResponseEntity<ProjectDto.ProjectResponse> updateProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID projectId,
            @Valid @RequestBody ProjectDto.UpdateProjectRequest request) {
        ProjectDto.ProjectResponse response = projectService.updateProject(projectId, principal.userId(), request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID projectId) {
        projectService.deleteProject(projectId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
