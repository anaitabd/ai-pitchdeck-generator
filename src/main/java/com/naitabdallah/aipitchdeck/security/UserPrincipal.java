package com.naitabdallah.aipitchdeck.security;

import java.util.UUID;

/**
 * User principal for authenticated users.
 */
public record UserPrincipal(UUID userId, String email, String role) {
}
