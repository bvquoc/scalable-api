package com.project.infrastructure.persistence.repository;

import com.project.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for UserRepository.
 * Tests use real PostgreSQL instance via Testcontainers.
 */
class UserRepositoryIntegrationTest extends BaseRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldSaveAndFindUserByEmail() {
        // Given
        UserEntity user = new UserEntity();
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setFullName("Test User");
        user.setStatus(UserEntity.UserStatus.ACTIVE);

        // When
        UserEntity saved = userRepository.save(user);
        Optional<UserEntity> found = userRepository.findByEmail("test@example.com");

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    void shouldFindUsersByStatus() {
        // Given
        UserEntity activeUser = new UserEntity();
        activeUser.setEmail("active@example.com");
        activeUser.setUsername("activeuser");
        activeUser.setStatus(UserEntity.UserStatus.ACTIVE);

        UserEntity inactiveUser = new UserEntity();
        inactiveUser.setEmail("inactive@example.com");
        inactiveUser.setUsername("inactiveuser");
        inactiveUser.setStatus(UserEntity.UserStatus.INACTIVE);

        userRepository.save(activeUser);
        userRepository.save(inactiveUser);

        // When
        List<UserEntity> activeUsers = userRepository.findByStatus(UserEntity.UserStatus.ACTIVE);
        List<UserEntity> inactiveUsers = userRepository.findByStatus(UserEntity.UserStatus.INACTIVE);

        // Then
        assertThat(activeUsers).isNotEmpty();
        assertThat(activeUsers).allMatch(u -> u.getStatus() == UserEntity.UserStatus.ACTIVE);
        assertThat(inactiveUsers).isNotEmpty();
        assertThat(inactiveUsers).allMatch(u -> u.getStatus() == UserEntity.UserStatus.INACTIVE);
    }

    @Test
    void shouldFindActiveUsers() {
        // Given
        UserEntity activeUser1 = new UserEntity();
        activeUser1.setEmail("active1@example.com");
        activeUser1.setUsername("active1");
        activeUser1.setStatus(UserEntity.UserStatus.ACTIVE);

        UserEntity activeUser2 = new UserEntity();
        activeUser2.setEmail("active2@example.com");
        activeUser2.setUsername("active2");
        activeUser2.setStatus(UserEntity.UserStatus.ACTIVE);

        UserEntity suspendedUser = new UserEntity();
        suspendedUser.setEmail("suspended@example.com");
        suspendedUser.setUsername("suspended");
        suspendedUser.setStatus(UserEntity.UserStatus.SUSPENDED);

        userRepository.save(activeUser1);
        userRepository.save(activeUser2);
        userRepository.save(suspendedUser);

        // When
        List<UserEntity> activeUsers = userRepository.findActiveUsers();

        // Then
        assertThat(activeUsers).isNotEmpty();
        assertThat(activeUsers).allMatch(u -> u.getStatus() == UserEntity.UserStatus.ACTIVE);
        assertThat(activeUsers).hasSize(2);
    }

    @Test
    void shouldCheckEmailExists() {
        // Given
        UserEntity user = new UserEntity();
        user.setEmail("exists@example.com");
        user.setUsername("existsuser");
        user.setStatus(UserEntity.UserStatus.ACTIVE);
        userRepository.save(user);

        // When
        boolean exists = userRepository.existsByEmail("exists@example.com");
        boolean notExists = userRepository.existsByEmail("notexists@example.com");

        // Then
        assertThat(exists).isTrue();
        assertThat(notExists).isFalse();
    }

    @Test
    void shouldSearchUsersByEmailOrUsername() {
        // Given
        UserEntity user1 = new UserEntity();
        user1.setEmail("john.doe@example.com");
        user1.setUsername("johndoe");
        user1.setStatus(UserEntity.UserStatus.ACTIVE);

        UserEntity user2 = new UserEntity();
        user2.setEmail("jane.smith@example.com");
        user2.setUsername("janesmith");
        user2.setStatus(UserEntity.UserStatus.ACTIVE);

        userRepository.save(user1);
        userRepository.save(user2);

        // When
        List<UserEntity> foundByEmail = userRepository.searchUsers("john");
        List<UserEntity> foundByUsername = userRepository.searchUsers("smith");

        // Then
        assertThat(foundByEmail).isNotEmpty();
        assertThat(foundByEmail).anyMatch(u -> u.getEmail().contains("john"));
        assertThat(foundByUsername).isNotEmpty();
        assertThat(foundByUsername).anyMatch(u -> u.getUsername().contains("smith"));
    }

    @Test
    void shouldAutoPopulateTimestamps() {
        // Given
        UserEntity user = new UserEntity();
        user.setEmail("timestamp@example.com");
        user.setUsername("timestampuser");
        user.setStatus(UserEntity.UserStatus.ACTIVE);

        // When
        UserEntity saved = userRepository.save(user);

        // Then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }
}
