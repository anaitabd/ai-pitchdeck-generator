package com.naitabdallah.aipitchdeck.mapper;

import com.naitabdallah.aipitchdeck.dto.PitchDeckDto;
import com.naitabdallah.aipitchdeck.entity.PitchDeck;
import org.springframework.stereotype.Component;

/**
 * Mapper for PitchDeck entity and DTOs.
 */
@Component
public class PitchDeckMapper {

    public PitchDeckDto.PitchDeckResponse toResponse(PitchDeck pitchDeck) {
        return new PitchDeckDto.PitchDeckResponse(
                pitchDeck.getId(),
                pitchDeck.getProjectId(),
                pitchDeck.getGenerationJobId(),
                pitchDeck.getTitle(),
                pitchDeck.getVersion(),
                pitchDeck.getContent(),
                pitchDeck.getSlideCount(),
                pitchDeck.getTemplateUsed(),
                pitchDeck.getIsCurrentVersion(),
                pitchDeck.getCreatedAt(),
                pitchDeck.getUpdatedAt()
        );
    }

    public PitchDeckDto.PitchDeckSummaryResponse toSummaryResponse(PitchDeck pitchDeck) {
        return new PitchDeckDto.PitchDeckSummaryResponse(
                pitchDeck.getId(),
                pitchDeck.getTitle(),
                pitchDeck.getVersion(),
                pitchDeck.getSlideCount(),
                pitchDeck.getIsCurrentVersion(),
                pitchDeck.getCreatedAt()
        );
    }
}
