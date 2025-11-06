package com.naitabdallah.aipitchdeck.mapper;

import com.naitabdallah.aipitchdeck.dto.UserResponse;
import com.naitabdallah.aipitchdeck.entity.User;
import org.springframework.stereotype.Component;

/**
 * Mapper for User entity and DTOs.
 */
@Component
public class UserMapper {

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCompany(),
                user.getRole().name(),
                user.getIsActive(),
                user.getEmailVerified(),
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }
}
