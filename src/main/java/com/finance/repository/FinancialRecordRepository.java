package com.finance.repository;

import com.finance.model.FinancialRecord;
import com.finance.model.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * FinancialRecord repository.
 *
 * Analytics queries are JPQL aggregations, keeping business logic out of Java code
 * and pushing computation down to the database — where it belongs and where it scales.
 */
@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    /**
     * Paginated, multi-filter query. All params are optional — passing null skips that filter.
     */
    @Query("""
            SELECT r FROM FinancialRecord r
            WHERE r.deletedAt IS NULL
              AND (:type     IS NULL OR r.type     = :type)
              AND (:category IS NULL OR LOWER(r.category) = LOWER(:category))
              AND (:from     IS NULL OR r.recordDate >= :from)
              AND (:to       IS NULL OR r.recordDate <= :to)
            ORDER BY r.recordDate DESC
            """)
    Page<FinancialRecord> findWithFilters(
            @Param("type")     RecordType type,
            @Param("category") String category,
            @Param("from")     LocalDate from,
            @Param("to")       LocalDate to,
            Pageable pageable
    );

    /** Find an active (non-deleted) record by ID. */
    @Query("SELECT r FROM FinancialRecord r WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<FinancialRecord> findActiveById(@Param("id") Long id);

    // -------------------------------------------------------------------------
    // Analytics queries — used by DashboardService
    // -------------------------------------------------------------------------

    /** Sum all INCOME entries in date range. Returns 0 if no records exist. */
    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM FinancialRecord r
            WHERE r.type = 'INCOME'
              AND r.deletedAt IS NULL
              AND (:from IS NULL OR r.recordDate >= :from)
              AND (:to   IS NULL OR r.recordDate <= :to)
            """)
    BigDecimal sumIncome(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Sum all EXPENSE entries in date range. */
    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
            FROM FinancialRecord r
            WHERE r.type = 'EXPENSE'
              AND r.deletedAt IS NULL
              AND (:from IS NULL OR r.recordDate >= :from)
              AND (:to   IS NULL OR r.recordDate <= :to)
            """)
    BigDecimal sumExpenses(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Category-wise totals — returns pairs of [category, sum].
     * Returned as Object[] for projection into a DTO.
     */
    @Query("""
            SELECT r.category, SUM(r.amount)
            FROM FinancialRecord r
            WHERE r.type = :type
              AND r.deletedAt IS NULL
              AND (:from IS NULL OR r.recordDate >= :from)
              AND (:to   IS NULL OR r.recordDate <= :to)
            GROUP BY r.category
            ORDER BY SUM(r.amount) DESC
            """)
    List<Object[]> getCategoryTotals(
            @Param("type") RecordType type,
            @Param("from") LocalDate from,
            @Param("to")   LocalDate to
    );

    /**
     * Monthly trend — returns [year, month, type, sum] rows.
     * Useful for time-series charts on the dashboard.
     */
    @Query("""
            SELECT YEAR(r.recordDate), MONTH(r.recordDate), r.type, SUM(r.amount)
            FROM FinancialRecord r
            WHERE r.deletedAt IS NULL
              AND (:from IS NULL OR r.recordDate >= :from)
              AND (:to   IS NULL OR r.recordDate <= :to)
            GROUP BY YEAR(r.recordDate), MONTH(r.recordDate), r.type
            ORDER BY YEAR(r.recordDate), MONTH(r.recordDate)
            """)
    List<Object[]> getMonthlyTrends(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Most recent N records for "recent activity" widget.
     */
    @Query("""
            SELECT r FROM FinancialRecord r
            WHERE r.deletedAt IS NULL
            ORDER BY r.recordDate DESC, r.createdAt DESC
            """)
    List<FinancialRecord> findRecentActivity(Pageable pageable);
}
