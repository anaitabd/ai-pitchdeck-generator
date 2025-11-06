package com.naitabdallah.aipitchdeck.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * LangChain4j configuration for AI model integration.
 */
@Configuration
public class LangChain4jConfig {

    @Value("${langchain4j.anthropic.api-key}")
    private String anthropicApiKey;

    @Value("${langchain4j.anthropic.model}")
    private String anthropicModel;

    @Value("${langchain4j.anthropic.timeout}")
    private Duration timeout;

    @Value("${langchain4j.anthropic.max-retries}")
    private int maxRetries;

    @Value("${langchain4j.anthropic.log-requests}")
    private boolean logRequests;

    @Value("${langchain4j.anthropic.log-responses}")
    private boolean logResponses;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return AnthropicChatModel.builder()
                .apiKey(anthropicApiKey)
                .modelName(anthropicModel)
                .timeout(timeout)
                .maxRetries(maxRetries)
                .logRequests(logRequests)
                .logResponses(logResponses)
                .build();
    }
}
