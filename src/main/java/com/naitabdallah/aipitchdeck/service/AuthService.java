package com.naitabdallah.aipitchdeck.service;

import com.naitabdallah.aipitchdeck.dto.AuthDto;
import com.naitabdallah.aipitchdeck.dto.UserResponse;
import com.naitabdallah.aipitchdeck.entity.RefreshToken;
import com.naitabdallah.aipitchdeck.entity.User;
import com.naitabdallah.aipitchdeck.exception.AuthenticationException;
import com.naitabdallah.aipitchdeck.exception.ResourceNotFoundException;
import com.naitabdallah.aipitchdeck.mapper.UserMapper;
import com.naitabdallah.aipitchdeck.repository.RefreshTokenRepository;
import com.naitabdallah.aipitchdeck.repository.UserRepository;
import com.naitabdallah.aipitchdeck.util.JwtUtil;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for authentication operations.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Transactional
    public AuthDto.LoginResponse register(AuthDto.RegisterRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthenticationException("Email already registered");
        }

        // Create new user
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .company(request.company())
                .role(User.UserRole.USER)
                .isActive(true)
                .emailVerified(false)
                .build();

        user = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        // Save refresh token
        saveRefreshToken(user.getId(), refreshToken);

        UserResponse userResponse = userMapper.toResponse(user);

        return new AuthDto.LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtUtil.getExpirationMs() / 1000,
                userResponse
        );
    }

    @Transactional
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            throw new AuthenticationException("Account is inactive");
        }

        // Update last login
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        // Save refresh token
        saveRefreshToken(user.getId(), refreshToken);

        UserResponse userResponse = userMapper.toResponse(user);

        return new AuthDto.LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtUtil.getExpirationMs() / 1000,
                userResponse
        );
    }

    @Transactional
    public AuthDto.RefreshTokenResponse refreshToken(AuthDto.RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new AuthenticationException("Invalid refresh token"));

        if (refreshToken.getRevoked()) {
            throw new AuthenticationException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new AuthenticationException("Refresh token has expired");
        }

        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", refreshToken.getUserId()));

        // Generate new access token
        String accessToken = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getRole().name());

        return new AuthDto.RefreshTokenResponse(
                accessToken,
                "Bearer",
                jwtUtil.getExpirationMs() / 1000
        );
    }

    @Transactional
    public void logout(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private void saveRefreshToken(UUID userId, String token) {
        Instant expiresAt = Instant.now().plusMillis(jwtUtil.getRefreshExpirationMs());

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(expiresAt)
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }
}
