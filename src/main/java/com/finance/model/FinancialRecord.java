package com.finance.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Financial record entity representing a single income or expense entry.
 *
 * Design notes:
 * - amount uses BigDecimal, never Double/Float. Floating-point types cannot
 *   represent decimal values exactly, which is unacceptable for financial data.
 *
 * - recordDate is LocalDate (not LocalDateTime). A transaction belongs to a
 *   calendar day; time-of-day is irrelevant for finance reporting.
 *
 * - category is a free-form String, not a foreign key to a Category table.
 *   Tradeoff: simpler schema + flexible input vs. no enforced taxonomy.
 *
 * - createdBy captures which user created the record for auditability.
 *
 * - Soft delete via deletedAt — records are never permanently removed.
 */
@Entity
@Table(name = "financial_records", indexes = {
        @Index(name = "idx_record_type",     columnList = "type"),
        @Index(name = "idx_record_date",     columnList = "record_date"),
        @Index(name = "idx_record_category", columnList = "category"),
        @Index(name = "idx_record_deleted",  columnList = "deleted_at")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Monetary amount. Always positive; the type field determines direction.
     * Using BigDecimal with DECIMAL(19,4) supports large amounts and 4 decimal places.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecordType type;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(length = 500)
    private String description;

    /** Who created this record — used for audit trail. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Soft delete timestamp. NULL = active record. */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
