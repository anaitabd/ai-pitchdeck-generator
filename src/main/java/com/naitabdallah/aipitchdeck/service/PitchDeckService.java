package com.naitabdallah.aipitchdeck.service;

import com.naitabdallah.aipitchdeck.dto.PitchDeckDto;
import com.naitabdallah.aipitchdeck.entity.PitchDeck;
import com.naitabdallah.aipitchdeck.exception.ResourceNotFoundException;
import com.naitabdallah.aipitchdeck.mapper.PitchDeckMapper;
import com.naitabdallah.aipitchdeck.repository.PitchDeckRepository;
import com.naitabdallah.aipitchdeck.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Service for pitch deck management.
 */
@Service
public class PitchDeckService {

    private final PitchDeckRepository pitchDeckRepository;
    private final ProjectRepository projectRepository;
    private final PitchDeckMapper pitchDeckMapper;

    public PitchDeckService(PitchDeckRepository pitchDeckRepository,
                            ProjectRepository projectRepository,
                            PitchDeckMapper pitchDeckMapper) {
        this.pitchDeckRepository = pitchDeckRepository;
        this.projectRepository = projectRepository;
        this.pitchDeckMapper = pitchDeckMapper;
    }

    public PitchDeckDto.PitchDeckResponse getPitchDeck(UUID pitchDeckId, UUID userId) {
        PitchDeck pitchDeck = pitchDeckRepository.findByIdAndUserId(pitchDeckId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("PitchDeck", "id", pitchDeckId));
        return pitchDeckMapper.toResponse(pitchDeck);
    }

    public PitchDeckDto.PitchDeckResponse getCurrentVersionByProject(UUID projectId, UUID userId) {
        // Verify project ownership
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        PitchDeck pitchDeck = pitchDeckRepository.findByProjectIdAndIsCurrentVersion(projectId, true)
                .orElseThrow(() -> new ResourceNotFoundException("No current pitch deck found for project", "projectId", projectId));

        return pitchDeckMapper.toResponse(pitchDeck);
    }

    public List<PitchDeckDto.PitchDeckSummaryResponse> getProjectVersions(UUID projectId, UUID userId) {
        // Verify project ownership
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        return pitchDeckRepository.findByProjectIdOrderByVersionDesc(projectId)
                .stream()
                .map(pitchDeckMapper::toSummaryResponse)
                .toList();
    }
}
