# Spring Boot Integration Guide

This guide shows how to integrate the AI Pitch Deck Generator Lambda function with the existing Spring Boot backend.

## Overview

The Lambda function is designed to be invoked asynchronously from the Spring Boot backend's `GenerationService`. The backend triggers the Lambda, which processes the request and posts results back via a callback endpoint.

## Architecture Flow

```
Spring Boot Backend                    AWS Lambda                     Backend Callback
─────────────────────                  ──────────                     ────────────────
1. User creates job
2. Upload files to S3
3. Invoke Lambda (async) ────────────► 4. Validate event
                                       5. Download from S3
                                       6. Generate pitch deck
                                       7. Upload result to S3
                                       8. POST callback ──────────► 9. Update job status
                                                                    10. Notify user
```

## Step 1: Add Lambda Invocation to GenerationService

Update `src/main/java/com/naitabdallah/aipitchdeck/service/GenerationService.java`:

```java
package com.naitabdallah.aipitchdeck.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvocationType;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenerationService {

    private final LambdaClient lambdaClient;
    private final GenerationJobRepository jobRepository;
    private final ObjectMapper objectMapper;

    @Value("${aws.lambda.function-name}")
    private String lambdaFunctionName;

    @Value("${server.url:http://localhost:8080}")
    private String serverUrl;

    /**
     * Trigger AI generation via Lambda function.
     */
    @Transactional
    public void triggerLambdaGeneration(GenerationJob job) {
        try {
            log.info("Triggering Lambda generation for job: {}", job.getId());

            // Build Lambda event payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("jobId", job.getId().toString());
            payload.put("projectId", job.getProjectId().toString());
            payload.put("userId", job.getUserId().toString());
            payload.put("s3Key", job.getInputS3Key());
            payload.put("callbackUrl", serverUrl + "/api/v1/generate/callback");
            payload.put("llmModel", job.getAiModel() != null ? job.getAiModel() : "claude-sonnet-4-20250514");
            
            // Optional: Add custom prompts
            if (job.getSystemPrompt() != null) {
                payload.put("systemPrompt", job.getSystemPrompt());
            }
            if (job.getUserPrompt() != null) {
                payload.put("userPrompt", job.getUserPrompt());
            }

            // Convert to JSON
            String jsonPayload = objectMapper.writeValueAsString(payload);
            log.debug("Lambda payload: {}", jsonPayload);

            // Invoke Lambda asynchronously
            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(lambdaFunctionName)
                    .invocationType(InvocationType.EVENT)  // Async invocation
                    .payload(SdkBytes.fromUtf8String(jsonPayload))
                    .build();

            InvokeResponse response = lambdaClient.invoke(invokeRequest);

            log.info("Lambda invoked successfully. Status: {}, RequestId: {}",
                    response.statusCode(), response.responseMetadata().requestId());

            // Update job status to PROCESSING
            job.setStatus(JobStatus.PROCESSING);
            jobRepository.save(job);

        } catch (Exception e) {
            log.error("Failed to invoke Lambda for job: {}", job.getId(), e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage("Failed to trigger Lambda: " + e.getMessage());
            jobRepository.save(job);
            throw new GenerationException("Failed to trigger Lambda generation", e);
        }
    }
}
```

## Step 2: Create Callback Controller

Create `src/main/java/com/naitabdallah/aipitchdeck/controller/GenerationCallbackController.java`:

```java
package com.naitabdallah.aipitchdeck.controller;

import com.naitabdallah.aipitchdeck.dto.GenerationDto;
import com.naitabdallah.aipitchdeck.service.GenerationCallbackService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/generate")
@RequiredArgsConstructor
@Slf4j
public class GenerationCallbackController {

    private final GenerationCallbackService callbackService;

    /**
     * Callback endpoint for Lambda function to report completion.
     */
    @PostMapping("/callback")
    public ResponseEntity<Void> handleLambdaCallback(
            @RequestBody LambdaCallbackRequest request) {
        
        log.info("Received Lambda callback: jobId={}, status={}", 
                request.jobId(), request.status());

        try {
            callbackService.processCallback(request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Error processing Lambda callback", e);
            // Return 200 anyway to prevent Lambda retries
            return ResponseEntity.ok().build();
        }
    }

    /**
     * Lambda callback request DTO.
     */
    public record LambdaCallbackRequest(
            UUID jobId,
            String status,
            String outputS3Key,
            String errorMessage,
            LocalDateTime generatedAt
    ) {}
}
```

## Step 3: Create Callback Service

Create `src/main/java/com/naitabdallah/aipitchdeck/service/GenerationCallbackService.java`:

```java
package com.naitabdallah.aipitchdeck.service;

import com.naitabdallah.aipitchdeck.controller.GenerationCallbackController.LambdaCallbackRequest;
import com.naitabdallah.aipitchdeck.entity.GenerationJob;
import com.naitabdallah.aipitchdeck.entity.PitchDeck;
import com.naitabdallah.aipitchdeck.repository.GenerationJobRepository;
import com.naitabdallah.aipitchdeck.repository.PitchDeckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class GenerationCallbackService {

    private final GenerationJobRepository jobRepository;
    private final PitchDeckRepository pitchDeckRepository;
    private final S3Service s3Service;

    @Transactional
    public void processCallback(LambdaCallbackRequest request) {
        log.info("Processing Lambda callback for job: {}", request.jobId());

        GenerationJob job = jobRepository.findById(request.jobId())
                .orElseThrow(() -> new ResourceNotFoundException("Job not found: " + request.jobId()));

        if ("COMPLETED".equals(request.status())) {
            handleSuccess(job, request);
        } else {
            handleFailure(job, request);
        }
    }

    private void handleSuccess(GenerationJob job, LambdaCallbackRequest request) {
        log.info("Job completed successfully: {}", job.getId());

        try {
            // Download result from S3
            String resultJson = s3Service.downloadFileAsString(request.outputS3Key());

            // Create PitchDeck entity
            PitchDeck pitchDeck = PitchDeck.builder()
                    .projectId(job.getProjectId())
                    .userId(job.getUserId())
                    .generationJobId(job.getId())
                    .content(resultJson)  // Store as JSONB
                    .s3Key(request.outputS3Key())
                    .version(1)
                    .build();

            pitchDeckRepository.save(pitchDeck);

            // Update job
            job.setStatus(JobStatus.COMPLETED);
            job.setOutputS3Key(request.outputS3Key());
            job.setPitchDeckId(pitchDeck.getId());
            job.setCompletedAt(request.generatedAt() != null ? 
                    request.generatedAt() : LocalDateTime.now());
            
            jobRepository.save(job);

            log.info("PitchDeck created: {}", pitchDeck.getId());

        } catch (Exception e) {
            log.error("Error processing successful callback", e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage("Failed to process result: " + e.getMessage());
            jobRepository.save(job);
        }
    }

    private void handleFailure(GenerationJob job, LambdaCallbackRequest request) {
        log.error("Job failed: {}, error: {}", job.getId(), request.errorMessage());

        job.setStatus(JobStatus.FAILED);
        job.setErrorMessage(request.errorMessage());
        job.setCompletedAt(LocalDateTime.now());
        
        jobRepository.save(job);
    }
}
```

## Step 4: Update GenerationController

Update `src/main/java/com/naitabdallah/aipitchdeck/controller/GenerationController.java`:

```java
@PostMapping("/start")
public ResponseEntity<GenerationDto.JobResponse> startGeneration(
        @Valid @RequestBody GenerationDto.StartRequest request,
        @AuthenticationPrincipal UserPrincipal principal) {
    
    log.info("Starting generation for project: {}", request.projectId());

    // Create job
    GenerationJob job = generationService.createJob(request, principal.getUserId());

    // Trigger Lambda asynchronously
    generationService.triggerLambdaGeneration(job);

    return ResponseEntity.ok(jobMapper.toResponse(job));
}
```

## Step 5: Update application.yaml

Add Lambda configuration if not already present:

```yaml
aws:
  lambda:
    function-name: ${AWS_LAMBDA_FUNCTION_NAME:ai-pitchdeck-generator}

server:
  url: ${SERVER_URL:http://localhost:8080}
```

## Step 6: Add S3 Download Helper (if needed)

Update `S3Service.java` to include a method to download result JSON:

```java
public String downloadFileAsString(String s3Key) {
    try {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        ResponseBytes<GetObjectResponse> responseBytes = s3Client.getObjectAsBytes(getRequest);
        return responseBytes.asUtf8String();

    } catch (S3Exception e) {
        log.error("Failed to download file from S3: {}", s3Key, e);
        throw new FileUploadException("Failed to download file from S3", e);
    }
}
```

## Testing the Integration

### 1. Local Testing with LocalStack

```bash
# Start LocalStack with Lambda support
docker-compose up -d

# Deploy Lambda to LocalStack
cd lambda
sam build
sam deploy --guided  # Use LocalStack endpoint
```

### 2. Integration Test

```java
@Test
void testLambdaIntegration() {
    // Create test project and upload file
    Project project = createTestProject();
    FileUpload file = uploadTestFile(project);

    // Start generation
    GenerationJob job = generationService.createJob(
            new StartRequest(project.getId(), List.of(file.getId())),
            user.getId()
    );

    // Trigger Lambda
    generationService.triggerLambdaGeneration(job);

    // Wait for callback (or use async testing)
    await().atMost(30, SECONDS).until(() -> {
        GenerationJob updated = jobRepository.findById(job.getId()).orElseThrow();
        return updated.getStatus() == JobStatus.COMPLETED;
    });

    // Verify result
    PitchDeck pitchDeck = pitchDeckRepository.findByProjectId(project.getId()).orElseThrow();
    assertNotNull(pitchDeck.getContent());
}
```

## Error Handling

The Lambda function will:
1. Retry callback up to 3 times with exponential backoff
2. Log all errors to CloudWatch
3. Send FAILED status if generation fails

The backend should:
1. Accept all callbacks (return 200) to prevent retries
2. Handle both COMPLETED and FAILED statuses
3. Log callback errors but continue processing

## Monitoring

### CloudWatch Metrics
- Monitor Lambda duration, errors, throttles
- Set up alarms for failures

### Backend Logs
- Log Lambda invocations
- Log callback receipts
- Track job status transitions

## Production Considerations

1. **Secrets**: Store Anthropic API key in AWS Secrets Manager
2. **VPC**: Deploy Lambda in VPC if accessing private resources
3. **Concurrency**: Set reserved concurrency limits
4. **Timeout**: Adjust based on average document size
5. **Dead Letter Queue**: Configure DLQ for failed invocations
6. **Monitoring**: Set up CloudWatch dashboards and alarms

## Alternative: Direct AI Generation (Without Lambda)

If you prefer to keep generation in the Spring Boot backend:

```java
// Use existing AIService instead of Lambda
@Async
public CompletableFuture<PitchDeck> generateAsync(GenerationJob job) {
    return CompletableFuture.supplyAsync(() -> {
        // Download file
        String content = s3Service.downloadFileAsString(job.getInputS3Key());
        
        // Generate pitch deck using LangChain4j
        String result = aiService.generatePitchDeck(content);
        
        // Save result
        return savePitchDeck(job, result);
    });
}
```

This approach is simpler but uses backend resources instead of dedicated Lambda compute.
