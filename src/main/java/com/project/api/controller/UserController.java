package com.project.api.controller;

import com.project.api.dto.CreateUserRequest;
import com.project.api.dto.UserResponse;
import com.project.domain.model.User;
import com.project.domain.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for User operations.
 *
 * Endpoints:
 * - POST   /api/users          - Create user
 * - GET    /api/users/{id}     - Get user by ID
 * - GET    /api/users          - List active users
 * - GET    /api/users/search   - Search users
 * - PUT    /api/users/{id}     - Update user
 * - DELETE /api/users/{id}     - Delete user
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management endpoints")
@SecurityRequirement(name = "apiKey")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Create a new user", description = "Creates a new user with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());
        user.setStatus(User.UserStatus.ACTIVE);

        User created = userService.createUser(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(created));
    }

    @Operation(summary = "Get user by ID", description = "Retrieves a user by their unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User found",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        return userService.getUserById(id)
                .map(UserResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "List active users", description = "Retrieves all active users in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of active users"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping
    public ResponseEntity<List<UserResponse>> getActiveUsers() {
        List<UserResponse> users = userService.getActiveUsers().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Search users", description = "Search users by email, username, or full name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @GetMapping("/search")
    public ResponseEntity<List<UserResponse>> searchUsers(
            @Parameter(description = "Search query", required = true) @RequestParam String q) {
        List<UserResponse> users = userService.searchUsers(q).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(users);
    }

    @Operation(summary = "Update user", description = "Updates an existing user's information")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id,
            @Valid @RequestBody CreateUserRequest request) {

        User user = new User();
        user.setEmail(request.getEmail());
        user.setUsername(request.getUsername());
        user.setFullName(request.getFullName());

        User updated = userService.updateUser(id, user);

        return ResponseEntity.ok(UserResponse.from(updated));
    }

    @Operation(summary = "Delete user", description = "Soft deletes a user by marking them as inactive")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid API key")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID", required = true) @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
