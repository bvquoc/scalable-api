package com.project.infrastructure.persistence.mapper;

import com.project.domain.model.User;
import com.project.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper to convert between UserEntity and User domain model.
 * Ensures clean separation between persistence layer and domain layer.
 */
@Component
public class UserMapper {

    /**
     * Convert UserEntity to User domain model.
     */
    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        User user = new User();
        user.setId(entity.getId());
        user.setEmail(entity.getEmail());
        user.setUsername(entity.getUsername());
        user.setFullName(entity.getFullName());
        user.setStatus(mapStatus(entity.getStatus()));
        user.setCreatedAt(entity.getCreatedAt());
        user.setUpdatedAt(entity.getUpdatedAt());

        return user;
    }

    /**
     * Convert User domain model to UserEntity.
     */
    public UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        UserEntity entity = new UserEntity();
        entity.setId(user.getId());
        entity.setEmail(user.getEmail());
        entity.setUsername(user.getUsername());
        entity.setFullName(user.getFullName());
        entity.setStatus(mapStatus(user.getStatus()));
        entity.setCreatedAt(user.getCreatedAt());
        entity.setUpdatedAt(user.getUpdatedAt());

        return entity;
    }

    /**
     * Update existing entity with domain model data.
     */
    public void updateEntity(UserEntity entity, User user) {
        if (entity == null || user == null) {
            return;
        }

        entity.setEmail(user.getEmail());
        entity.setUsername(user.getUsername());
        entity.setFullName(user.getFullName());
        entity.setStatus(mapStatus(user.getStatus()));
    }

    private User.UserStatus mapStatus(UserEntity.UserStatus entityStatus) {
        if (entityStatus == null) {
            return null;
        }
        return User.UserStatus.valueOf(entityStatus.name());
    }

    private UserEntity.UserStatus mapStatus(User.UserStatus domainStatus) {
        if (domainStatus == null) {
            return null;
        }
        return UserEntity.UserStatus.valueOf(domainStatus.name());
    }
}
