package com.naitabdallah.aipitchdeck.entity;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * GenerationJob entity representing AI pitch deck generation jobs.
 */
@Entity
@Table(name = "generation_jobs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerationJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private JobStatus status = JobStatus.QUEUED;

    @Column(name = "ai_model", nullable = false, length = 100)
    private String aiModel;

    @Column(name = "prompt_template", length = 100)
    private String promptTemplate;

    @Column(name = "input_file_ids", nullable = false, columnDefinition = "uuid[]")
    private UUID[] inputFileIds;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum JobStatus {
        QUEUED, PROCESSING, COMPLETED, FAILED, CANCELLED
    }
}
