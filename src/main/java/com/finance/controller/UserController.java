package com.finance.controller;

import com.finance.dto.request.CreateUserRequest;
import com.finance.dto.request.UpdateUserRequest;
import com.finance.dto.response.PagedResponse;
import com.finance.dto.response.UserResponse;
import com.finance.model.Role;
import com.finance.model.UserStatus;
import com.finance.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User management endpoints.
 *
 * All endpoints require authentication (enforced at the SecurityConfig level).
 * Role restrictions are enforced in the service layer via @PreAuthorize.
 *
 * URL design:
 *   GET    /api/users          → list all users          (ADMIN)
 *   POST   /api/users          → create user             (ADMIN)
 *   GET    /api/users/me       → own profile             (ALL)
 *   GET    /api/users/{id}     → get user by id          (ADMIN)
 *   PATCH  /api/users/{id}     → partial update          (ADMIN)
 *   DELETE /api/users/{id}     → soft delete             (ADMIN)
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User management — Admin only except /me")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "List all users (paginated)", description = "Admin only. Supports filtering by role and status.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Paginated user list"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions")
    })
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @Parameter(description = "Filter by role") @RequestParam(required = false) Role role,
            @Parameter(description = "Filter by status") @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(userService.getAllUsers(role, status, page, size));
    }

    @GetMapping("/me")
    @Operation(summary = "Get your own profile", description = "Available to all authenticated users.")
    public ResponseEntity<UserResponse> getMyProfile() {
        return ResponseEntity.ok(userService.getMyProfile());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID", description = "Admin only.")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new user", description = "Admin only.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "409", description = "Email already in use"),
        @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(request));
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Update user (partial)",
        description = "Admin only. Only provided fields are updated. Omitted fields remain unchanged."
    )
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a user", description = "Admin only. Sets deletedAt; does not remove the row.")
    @ApiResponse(responseCode = "204", description = "User deleted")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
