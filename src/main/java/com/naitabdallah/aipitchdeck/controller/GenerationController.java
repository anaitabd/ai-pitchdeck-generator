package com.naitabdallah.aipitchdeck.controller;

import com.naitabdallah.aipitchdeck.dto.GenerationDto;
import com.naitabdallah.aipitchdeck.security.UserPrincipal;
import com.naitabdallah.aipitchdeck.service.GenerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for AI generation operations.
 */
@RestController
@RequestMapping("/api/v1/generate")
public class GenerationController {

    private final GenerationService generationService;

    public GenerationController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/start")
    public ResponseEntity<GenerationDto.GenerationJobResponse> startGeneration(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody GenerationDto.StartGenerationRequest request) {
        GenerationDto.GenerationJobResponse response = generationService.startGeneration(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/job/{jobId}")
    public ResponseEntity<GenerationDto.GenerationJobResponse> getJobStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        GenerationDto.GenerationJobResponse response = generationService.getJobStatus(jobId, principal.userId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}/jobs")
    public ResponseEntity<List<GenerationDto.GenerationJobResponse>> getProjectJobs(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID projectId) {
        List<GenerationDto.GenerationJobResponse> response = generationService.getProjectJobs(projectId, principal.userId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/job/{jobId}/cancel")
    public ResponseEntity<Void> cancelJob(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID jobId) {
        generationService.cancelJob(jobId, principal.userId());
        return ResponseEntity.noContent().build();
    }
}
