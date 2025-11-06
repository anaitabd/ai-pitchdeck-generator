package com.naitabdallah.aipitchdeck.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * PitchDeck entity representing generated pitch deck content.
 * Supports versioning to track changes over time.
 */
@Entity
@Table(name = "pitch_decks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PitchDeck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "generation_job_id")
    private UUID generationJobId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false)
    @Builder.Default
    private Integer version = 1;

    @Column(nullable = false, columnDefinition = "jsonb")
    private String content;

    @Column(name = "slide_count", nullable = false)
    private Integer slideCount;

    @Column(name = "template_used", length = 100)
    private String templateUsed;

    @Column(name = "is_current_version", nullable = false)
    @Builder.Default
    private Boolean isCurrentVersion = true;

    @Column(columnDefinition = "jsonb")
    private String metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
