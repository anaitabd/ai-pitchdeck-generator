package com.naitabdallah.aipitchdeck.repository;

import com.naitabdallah.aipitchdeck.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for RefreshToken entity operations.
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    
    Optional<RefreshToken> findByToken(String token);
    
    void deleteByUserId(UUID userId);
    
    void deleteByExpiresAtBefore(Instant now);
}
