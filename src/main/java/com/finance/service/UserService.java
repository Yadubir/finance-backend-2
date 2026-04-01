package com.finance.service;

import com.finance.dto.request.CreateUserRequest;
import com.finance.dto.request.UpdateUserRequest;
import com.finance.dto.response.PagedResponse;
import com.finance.dto.response.UserResponse;
import com.finance.exception.BusinessException;
import com.finance.exception.DuplicateResourceException;
import com.finance.exception.ResourceNotFoundException;
import com.finance.model.Role;
import com.finance.model.User;
import com.finance.model.UserStatus;
import com.finance.repository.UserRepository;
import com.finance.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * User management service.
 *
 * Access control is enforced via @PreAuthorize annotations (method security).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Paginated user list. Admin only.
     *
     * @param role   filter by role (optional)
     * @param status filter by status (optional)
     * @param page   0-indexed page number
     * @param size   page size (max 100)
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> getAllUsers(Role role, UserStatus status, int page, int size) {
        size = Math.min(size, 100); // guard against huge page requests
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return PagedResponse.from(
                userRepository.findAllActive(role, status, pageable).map(this::toResponse)
        );
    }

    /** Get a single user by ID. Admin only. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return toResponse(findActiveUserOrThrow(id));
    }

    /**
     * Any authenticated user can view their own profile.
     * No role restriction here — the user ID is taken from the JWT, not the path.
     */
    @Transactional(readOnly = true)
    public UserResponse getMyProfile() {
        UserPrincipal principal = getCurrentPrincipal();
        return toResponse(findActiveUserOrThrow(principal.getId()));
    }

    // Mutations

    /** Create a new user. Admin only. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(
                    "A user with email '" + request.getEmail() + "' already exists"
            );
        }

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole())
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(user);
        log.info("Admin created user: {} with role: {}", saved.getEmail(), saved.getRole());
        return toResponse(saved);
    }

    /**
     * Update a user's profile fields. Admin only.
     * Partial update — only non-null fields from the request are applied.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findActiveUserOrThrow(id);

        // Business rule: prevent the last ACTIVE admin from being demoted or deactivated
        if (user.getRole() == Role.ADMIN) {
            boolean isDemoting    = request.getRole()   != null && request.getRole()   != Role.ADMIN;
            boolean isDeactivating= request.getStatus() != null && request.getStatus() != UserStatus.ACTIVE;
            if (isDemoting || isDeactivating) {
                long activeAdminCount = userRepository.findAllActive(Role.ADMIN, UserStatus.ACTIVE,
                        PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
                if (activeAdminCount <= 1) {
                    throw new BusinessException("Cannot demote or deactivate the last active admin");
                }
            }
        }

        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getRole()     != null) user.setRole(request.getRole());
        if (request.getStatus()   != null) user.setStatus(request.getStatus());

        log.info("Admin updated user id: {}", id);
        return toResponse(userRepository.save(user));
    }

    /**
     * Soft-delete a user. Admin only.
     * Sets deletedAt timestamp instead of physically removing the row.
     * The user's audit trail (created records, etc.) is preserved.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteUser(Long id) {
        User user = findActiveUserOrThrow(id);

        // Business rule: cannot delete your own account
        UserPrincipal principal = getCurrentPrincipal();
        if (principal.getId().equals(id)) {
            throw new BusinessException("You cannot delete your own account");
        }

        // Business rule: cannot delete the last active admin
        if (user.getRole() == Role.ADMIN) {
            long activeAdminCount = userRepository.findAllActive(Role.ADMIN, UserStatus.ACTIVE,
                    PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
            if (activeAdminCount <= 1) {
                throw new BusinessException("Cannot delete the last active admin account");
            }
        }

        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
        log.info("Admin soft-deleted user id: {}", id);
    }

    //  Helpers 

    private User findActiveUserOrThrow(Long id) {
        return userRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private UserPrincipal getCurrentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    /** Maps User entity → UserResponse DTO (never exposes passwordHash). */
    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
