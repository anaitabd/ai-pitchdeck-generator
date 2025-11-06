package com.naitabdallah.aipitchdeck.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for User responses.
 */
public record UserResponse(
        UUID id,
        String email,
        String firstName,
        String lastName,
        String company,
        String role,
        Boolean isActive,
        Boolean emailVerified,
        Instant createdAt,
        Instant lastLoginAt
) {}
