package com.naitabdallah.aipitchdeck.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naitabdallah.aipitchdeck.dto.GenerationDto;
import com.naitabdallah.aipitchdeck.entity.*;
import com.naitabdallah.aipitchdeck.exception.GenerationException;
import com.naitabdallah.aipitchdeck.exception.ResourceNotFoundException;
import com.naitabdallah.aipitchdeck.mapper.GenerationJobMapper;
import com.naitabdallah.aipitchdeck.repository.*;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing AI pitch deck generation workflow.
 */
@Service
public class GenerationService {

    private static final Logger logger = LoggerFactory.getLogger(GenerationService.class);

    private final GenerationJobRepository generationJobRepository;
    private final ProjectRepository projectRepository;
    private final FileUploadRepository fileUploadRepository;
    private final PitchDeckRepository pitchDeckRepository;
    private final AIService aiService;
    private final GenerationJobMapper generationJobMapper;
    private final ObjectMapper objectMapper;

    @Value("${langchain4j.anthropic.model}")
    private String aiModel;

    public GenerationService(GenerationJobRepository generationJobRepository,
                             ProjectRepository projectRepository,
                             FileUploadRepository fileUploadRepository,
                             PitchDeckRepository pitchDeckRepository,
                             AIService aiService,
                             GenerationJobMapper generationJobMapper,
                             ObjectMapper objectMapper) {
        this.generationJobRepository = generationJobRepository;
        this.projectRepository = projectRepository;
        this.fileUploadRepository = fileUploadRepository;
        this.pitchDeckRepository = pitchDeckRepository;
        this.aiService = aiService;
        this.generationJobMapper = generationJobMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public GenerationDto.GenerationJobResponse startGeneration(UUID userId, GenerationDto.StartGenerationRequest request) {
        // Validate project ownership
        Project project = projectRepository.findByIdAndUserId(request.projectId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.projectId()));

        // Validate files
        List<FileUpload> files = fileUploadRepository.findAllById(request.fileIds());
        if (files.size() != request.fileIds().size()) {
            throw new GenerationException("Some files not found");
        }

        // Verify all files belong to the project
        for (FileUpload file : files) {
            if (!file.getProjectId().equals(request.projectId())) {
                throw new GenerationException("File does not belong to the project: " + file.getId());
            }
            if (file.getUploadStatus() != FileUpload.UploadStatus.COMPLETED) {
                throw new GenerationException("File upload not completed: " + file.getId());
            }
        }

        // Create generation job
        GenerationJob job = GenerationJob.builder()
                .projectId(request.projectId())
                .userId(userId)
                .status(GenerationJob.JobStatus.QUEUED)
                .aiModel(aiModel)
                .promptTemplate(request.promptTemplate())
                .inputFileIds(request.fileIds().toArray(new UUID[0]))
                .retryCount(0)
                .maxRetries(3)
                .build();

        job = generationJobRepository.save(job);

        // Start async processing
        processGenerationAsync(job.getId());

        return generationJobMapper.toResponse(job);
    }

    @Async("generationTaskExecutor")
    public void processGenerationAsync(UUID jobId) {
        logger.info("Starting async generation for job: {}", jobId);
        
        try {
            processGeneration(jobId);
        } catch (Exception e) {
            logger.error("Error in async generation processing: {}", e.getMessage(), e);
        }
    }

    @Transactional
    public void processGeneration(UUID jobId) {
        GenerationJob job = generationJobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("GenerationJob", "id", jobId));

        try {
            // Update job status
            job.setStatus(GenerationJob.JobStatus.PROCESSING);
            job.setStartedAt(Instant.now());
            generationJobRepository.save(job);

            // Get project details
            Project project = projectRepository.findById(job.getProjectId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", job.getProjectId()));

            // Get files and extract content
            List<FileUpload> files = fileUploadRepository.findAllById(List.of(job.getInputFileIds()));
            StringBuilder combinedContent = new StringBuilder();

            for (FileUpload file : files) {
                String content = aiService.extractTextFromFile(file.getS3Key(), file.getFileType().name());
                combinedContent.append(content).append("\n\n");
            }

            // Generate pitch deck using AI
            String aiResponse = aiService.generatePitchDeck(
                    combinedContent.toString(),
                    project.getDescription(),
                    project.getIndustry(),
                    project.getTargetAudience()
            );

            // Parse and validate JSON response
            JsonNode pitchDeckJson = objectMapper.readTree(aiResponse);
            int slideCount = pitchDeckJson.get("slides").size();

            // Unset current version flag for existing pitch decks
            pitchDeckRepository.unsetCurrentVersionForProject(project.getId());

            // Get next version number
            long versionCount = pitchDeckRepository.countByProjectId(project.getId());
            int nextVersion = (int) (versionCount + 1);

            // Save pitch deck
            PitchDeck pitchDeck = PitchDeck.builder()
                    .projectId(project.getId())
                    .generationJobId(job.getId())
                    .userId(job.getUserId())
                    .title(pitchDeckJson.get("title").asText())
                    .version(nextVersion)
                    .content(aiResponse)
                    .slideCount(slideCount)
                    .templateUsed("default")
                    .isCurrentVersion(true)
                    .metadata("{}")
                    .build();

            pitchDeckRepository.save(pitchDeck);

            // Update job status
            job.setStatus(GenerationJob.JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            generationJobRepository.save(job);

            logger.info("Generation completed successfully for job: {}", jobId);

        } catch (Exception e) {
            logger.error("Generation failed for job {}: {}", jobId, e.getMessage(), e);

            // Update job with error
            job.setStatus(GenerationJob.JobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setCompletedAt(Instant.now());

            // Retry logic
            if (job.getRetryCount() < job.getMaxRetries()) {
                job.setRetryCount(job.getRetryCount() + 1);
                job.setStatus(GenerationJob.JobStatus.QUEUED);
                generationJobRepository.save(job);
                
                logger.info("Retrying generation for job: {} (attempt {}/{})", 
                        jobId, job.getRetryCount(), job.getMaxRetries());
                processGenerationAsync(jobId);
            } else {
                generationJobRepository.save(job);
                logger.error("Max retries exceeded for job: {}", jobId);
            }
        }
    }

    public GenerationDto.GenerationJobResponse getJobStatus(UUID jobId, UUID userId) {
        GenerationJob job = generationJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("GenerationJob", "id", jobId));
        return generationJobMapper.toResponse(job);
    }

    public List<GenerationDto.GenerationJobResponse> getProjectJobs(UUID projectId, UUID userId) {
        // Verify project ownership
        projectRepository.findByIdAndUserId(projectId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));

        return generationJobRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(generationJobMapper::toResponse)
                .toList();
    }

    @Transactional
    public void cancelJob(UUID jobId, UUID userId) {
        GenerationJob job = generationJobRepository.findByIdAndUserId(jobId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("GenerationJob", "id", jobId));

        if (job.getStatus() == GenerationJob.JobStatus.COMPLETED || 
            job.getStatus() == GenerationJob.JobStatus.FAILED) {
            throw new GenerationException("Cannot cancel a completed or failed job");
        }

        job.setStatus(GenerationJob.JobStatus.CANCELLED);
        job.setCompletedAt(Instant.now());
        generationJobRepository.save(job);
    }
}
