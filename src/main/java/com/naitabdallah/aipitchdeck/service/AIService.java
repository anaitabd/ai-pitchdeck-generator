package com.naitabdallah.aipitchdeck.service;

import com.naitabdallah.aipitchdeck.exception.GenerationException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for AI-powered pitch deck generation using LangChain4j.
 */
@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);

    private final ChatLanguageModel chatModel;

    public AIService(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Generate pitch deck content using AI based on uploaded files.
     */
    public String generatePitchDeck(String documentContent, String projectDescription, String industry, String targetAudience) {
        try {
            String prompt = buildPitchDeckPrompt(documentContent, projectDescription, industry, targetAudience);
            logger.debug("Sending prompt to AI model");
            
            String response = chatModel.generate(prompt);
            logger.debug("Received response from AI model");
            
            return response;
        } catch (Exception e) {
            logger.error("Error generating pitch deck with AI: {}", e.getMessage(), e);
            throw new GenerationException("Failed to generate pitch deck with AI", e);
        }
    }

    /**
     * Build the prompt for AI pitch deck generation.
     */
    private String buildPitchDeckPrompt(String documentContent, String projectDescription, String industry, String targetAudience) {
        return String.format("""
                You are an expert pitch deck creator. Based on the following information, generate a comprehensive pitch deck in JSON format.
                
                PROJECT DESCRIPTION:
                %s
                
                INDUSTRY:
                %s
                
                TARGET AUDIENCE:
                %s
                
                DOCUMENT CONTENT:
                %s
                
                Generate a pitch deck with 10-15 slides in the following JSON structure:
                {
                  "title": "Pitch Deck Title",
                  "slides": [
                    {
                      "slideNumber": 1,
                      "title": "Slide Title",
                      "type": "cover|problem|solution|product|market|business-model|competition|traction|team|financials|ask|closing",
                      "content": {
                        "headline": "Main headline",
                        "bulletPoints": ["Point 1", "Point 2", "Point 3"],
                        "notes": "Speaker notes or additional context"
                      }
                    }
                  ],
                  "metadata": {
                    "version": "1.0",
                    "generatedAt": "%s",
                    "industry": "%s",
                    "targetAudience": "%s"
                  }
                }
                
                Focus on:
                1. Clear problem statement
                2. Compelling solution
                3. Market opportunity
                4. Business model viability
                5. Competitive advantage
                6. Team strengths
                7. Financial projections
                8. Clear ask/call-to-action
                
                Return ONLY the JSON, no additional text.
                """,
                projectDescription != null ? projectDescription : "Not provided",
                industry != null ? industry : "General",
                targetAudience != null ? targetAudience : "Investors",
                truncateContent(documentContent, 4000),
                java.time.Instant.now().toString(),
                industry,
                targetAudience
        );
    }

    /**
     * Truncate content to avoid token limits.
     */
    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength) + "... [truncated]";
    }

    /**
     * Extract text content from file (placeholder for actual implementation).
     * In a real implementation, this would use libraries like Apache POI, PDFBox, etc.
     */
    public String extractTextFromFile(String s3Key, String fileType) {
        // This is a placeholder. In a real implementation:
        // 1. Download file from S3
        // 2. Use appropriate library to extract text:
        //    - PDFBox for PDF
        //    - Apache POI for DOC/DOCX
        //    - Simple file reading for TXT/MD
        // 3. Return extracted text
        
        logger.info("Extracting text from file: {} (type: {})", s3Key, fileType);
        
        // For now, return a placeholder
        return "Sample document content extracted from " + fileType + " file.";
    }
}
