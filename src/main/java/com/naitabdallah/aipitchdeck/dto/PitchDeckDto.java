package com.naitabdallah.aipitchdeck.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTOs for PitchDeck operations.
 */
public class PitchDeckDto {

    public record PitchDeckResponse(
            UUID id,
            UUID projectId,
            UUID generationJobId,
            String title,
            Integer version,
            String content,
            Integer slideCount,
            String templateUsed,
            Boolean isCurrentVersion,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record PitchDeckSummaryResponse(
            UUID id,
            String title,
            Integer version,
            Integer slideCount,
            Boolean isCurrentVersion,
            Instant createdAt
    ) {}
}
