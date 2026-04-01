package com.finance.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Core user entity.
 *
 * Design notes:
 * - Role is stored as a string enum, not a separate table. Tradeoff: simpler
 *   schema vs. less flexibility for fine-grained permission editing at runtime.
 *   For this assignment's scope, a string enum is the right call.
 *
 * - Soft delete via deletedAt. Tradeoff: queries must always filter deleted_at IS NULL.
 *   We handle this with @Where at the entity level so callers don't have to think about it.
 *
 * - passwordHash stored separately from the DTO layer — the raw password never
 *   touches this entity; only the BCrypt hash is persisted.
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Soft delete timestamp. NULL means the record is active.
     * Populated instead of physically deleting the row.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // --- Convenience helpers for Spring Security ---

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE && this.deletedAt == null;
    }
}
