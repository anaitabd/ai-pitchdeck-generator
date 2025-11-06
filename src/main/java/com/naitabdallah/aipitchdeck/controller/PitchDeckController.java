package com.naitabdallah.aipitchdeck.controller;

import com.naitabdallah.aipitchdeck.dto.PitchDeckDto;
import com.naitabdallah.aipitchdeck.security.UserPrincipal;
import com.naitabdallah.aipitchdeck.service.PitchDeckService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for pitch deck operations.
 */
@RestController
@RequestMapping("/api/v1/pitch-decks")
public class PitchDeckController {

    private final PitchDeckService pitchDeckService;

    public PitchDeckController(PitchDeckService pitchDeckService) {
        this.pitchDeckService = pitchDeckService;
    }

    @GetMapping("/{pitchDeckId}")
    public ResponseEntity<PitchDeckDto.PitchDeckResponse> getPitchDeck(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID pitchDeckId) {
        PitchDeckDto.PitchDeckResponse response = pitchDeckService.getPitchDeck(pitchDeckId, principal.userId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}/current")
    public ResponseEntity<PitchDeckDto.PitchDeckResponse> getCurrentVersion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID projectId) {
        PitchDeckDto.PitchDeckResponse response = pitchDeckService.getCurrentVersionByProject(projectId, principal.userId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/project/{projectId}/versions")
    public ResponseEntity<List<PitchDeckDto.PitchDeckSummaryResponse>> getProjectVersions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID projectId) {
        List<PitchDeckDto.PitchDeckSummaryResponse> response = pitchDeckService.getProjectVersions(projectId, principal.userId());
        return ResponseEntity.ok(response);
    }
}
