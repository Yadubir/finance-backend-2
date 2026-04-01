package com.finance.model;

/**
 * Role hierarchy:
 *
 *   VIEWER   → read-only dashboard access
 *   ANALYST  → read records + access analytics/insights
 *   ADMIN    → full CRUD on records + user management
 *
 * Tradeoff: A flat enum is simple and readable. If requirements grow to include
 * dozens of granular permissions (e.g. "can_export", "can_approve"), consider
 * a Permission entity with a many-to-many join. For this scope, enum is ideal.
 */
public enum Role {
    VIEWER,
    ANALYST,
    ADMIN
}
