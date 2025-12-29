package com.project.domain.service;

import com.project.domain.model.User;
import com.project.infrastructure.persistence.entity.UserEntity;
import com.project.infrastructure.persistence.mapper.UserMapper;
import com.project.infrastructure.persistence.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service layer for User domain operations.
 * Orchestrates between controllers, repositories, and messaging.
 */
@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * Create new user.
     */
    public User createUser(User user) {
        log.info("Creating user: email={}", user.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("User with email already exists: " + user.getEmail());
        }

        UserEntity entity = userMapper.toEntity(user);
        UserEntity saved = userRepository.save(entity);

        log.info("User created successfully: id={}, email={}", saved.getId(), saved.getEmail());

        return userMapper.toDomain(saved);
    }

    /**
     * Get user by ID.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        log.debug("Fetching user by id: {}", id);

        return userRepository.findById(id)
                .map(userMapper::toDomain);
    }

    /**
     * Get user by email.
     */
    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        log.debug("Fetching user by email: {}", email);

        return userRepository.findByEmail(email)
                .map(userMapper::toDomain);
    }

    /**
     * Get all active users.
     */
    @Transactional(readOnly = true)
    public List<User> getActiveUsers() {
        log.debug("Fetching all active users");

        return userRepository.findActiveUsers().stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Search users by email or username.
     */
    @Transactional(readOnly = true)
    public List<User> searchUsers(String searchTerm) {
        log.debug("Searching users: searchTerm={}", searchTerm);

        return userRepository.searchUsers(searchTerm).stream()
                .map(userMapper::toDomain)
                .collect(Collectors.toList());
    }

    /**
     * Update user.
     */
    public User updateUser(Long id, User user) {
        log.info("Updating user: id={}", id);

        UserEntity entity = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));

        userMapper.updateEntity(entity, user);
        UserEntity updated = userRepository.save(entity);

        log.info("User updated successfully: id={}", id);

        return userMapper.toDomain(updated);
    }

    /**
     * Delete user.
     */
    public void deleteUser(Long id) {
        log.info("Deleting user: id={}", id);

        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("User not found: " + id);
        }

        userRepository.deleteById(id);

        log.info("User deleted successfully: id={}", id);
    }
}
