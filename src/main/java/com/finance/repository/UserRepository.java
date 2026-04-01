package com.finance.repository;

import com.finance.model.Role;
import com.finance.model.User;
import com.finance.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User repository.
 *
 * All queries explicitly filter out soft-deleted users (deleted_at IS NULL).
 * Tradeoff: we do this at query level rather than using Hibernate @Where so
 * that we can still fetch deleted users when genuinely needed (e.g. audit logs).
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find an active (non-deleted) user by email.
     * Used by Spring Security's UserDetailsService during authentication.
     */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findActiveByEmail(@Param("email") String email);

    /**
     * Check email uniqueness before creating a new user.
     * Checks all users including soft-deleted to prevent email reuse.
     */
    boolean existsByEmail(String email);

    /**
     * Paginated list of all non-deleted users, with optional role and status filters.
     */
    @Query("""
            SELECT u FROM User u
            WHERE u.deletedAt IS NULL
              AND (:role IS NULL OR u.role = :role)
              AND (:status IS NULL OR u.status = :status)
            """)
    Page<User> findAllActive(
            @Param("role")   Role role,
            @Param("status") UserStatus status,
            Pageable pageable
    );

    /**
     * Find a specific active user by ID.
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveById(@Param("id") Long id);
}
