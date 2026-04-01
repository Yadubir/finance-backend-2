package com.finance.service;

import com.finance.dto.response.DashboardSummaryResponse;
import com.finance.dto.response.DashboardSummaryResponse.CategoryTotal;
import com.finance.dto.response.DashboardSummaryResponse.MonthlyTrend;
import com.finance.model.RecordType;
import com.finance.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard analytics service.
 *
 * Design: All aggregation happens in the database via JPQL GROUP BY queries.
 * This is more efficient than loading records into memory and aggregating in Java.
 *
 * Access control:
 *   - VIEWER can see the summary (totals, balances)
 *   - ANALYST and ADMIN can see everything including category breakdowns and trends
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final FinancialRecordRepository recordRepository;
    private final FinancialRecordService    recordService;

    /**
     * Full dashboard summary. Accessible by ANALYST and ADMIN.
     *
     * @param from start date for the reporting period (optional — defaults to no lower bound)
     * @param to   end date for the reporting period (optional — defaults to today)
     */
    @PreAuthorize("hasAnyRole('ANALYST', 'ADMIN')")
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getFullSummary(LocalDate from, LocalDate to) {
        BigDecimal income   = recordRepository.sumIncome(from, to);
        BigDecimal expenses = recordRepository.sumExpenses(from, to);
        BigDecimal net      = income.subtract(expenses);

        return DashboardSummaryResponse.builder()
                .totalIncome(income)
                .totalExpenses(expenses)
                .netBalance(net)
                .incomeByCateogry(buildCategoryTotals(RecordType.INCOME,  from, to))
                .expensesByCategory(buildCategoryTotals(RecordType.EXPENSE, from, to))
                .monthlyTrends(buildMonthlyTrends(from, to))
                .recentActivity(buildRecentActivity())
                .build();
    }

    /**
     * Lightweight summary — totals only. Accessible by all roles including VIEWER.
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Transactional(readOnly = true)
    public Map<String, Object> getBasicSummary(LocalDate from, LocalDate to) {
        BigDecimal income   = recordRepository.sumIncome(from, to);
        BigDecimal expenses = recordRepository.sumExpenses(from, to);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalIncome",   income);
        summary.put("totalExpenses", expenses);
        summary.put("netBalance",    income.subtract(expenses));
        summary.put("periodFrom",    from);
        summary.put("periodTo",      to);
        return summary;
    }

    // ── Private aggregation helpers ───────────────────────────────────────────

    /**
     * Converts raw Object[] rows from getCategoryTotals() into CategoryTotal DTOs.
     * Raw query returns: [String category, BigDecimal sum]
     */
    private List<CategoryTotal> buildCategoryTotals(RecordType type, LocalDate from, LocalDate to) {
        return recordRepository.getCategoryTotals(type, from, to)
                .stream()
                .map(row -> CategoryTotal.builder()
                        .category((String)     row[0])
                        .total((BigDecimal) row[1])
                        .build()
                )
                .collect(Collectors.toList());
    }

    /**
     * Builds monthly income/expense trend data.
     *
     * The query returns one row per (year, month, type) combination.
     * We pivot the data here so each output row contains both income and expense
     * for the same month — convenient for line charts.
     *
     * Raw query returns: [Integer year, Integer month, RecordType type, BigDecimal sum]
     */
    private List<MonthlyTrend> buildMonthlyTrends(LocalDate from, LocalDate to) {
        List<Object[]> rows = recordRepository.getMonthlyTrends(from, to);

        // Use a LinkedHashMap keyed by "YYYY-MM" to preserve chronological order
        Map<String, MonthlyTrend> trendMap = new LinkedHashMap<>();

        for (Object[] row : rows) {
            int        year   = ((Number) row[0]).intValue();
            int        month  = ((Number) row[1]).intValue();
            RecordType type   = RecordType.valueOf(row[2].toString());
            BigDecimal amount = (BigDecimal) row[3];

            String key   = String.format("%d-%02d", year, month);
            String label = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                           + " " + year;  // e.g. "Jan 2024"

            MonthlyTrend trend = trendMap.computeIfAbsent(key, k ->
                    MonthlyTrend.builder()
                            .year(year).month(month).monthLabel(label)
                            .income(BigDecimal.ZERO).expenses(BigDecimal.ZERO)
                            .build()
            );

            if (type == RecordType.INCOME) {
                trend.setIncome(amount);
            } else {
                trend.setExpenses(amount);
            }
            trend.setNet(trend.getIncome().subtract(trend.getExpenses()));
        }

        return new ArrayList<>(trendMap.values());
    }

    /** Fetches the 10 most recent records for the "Recent Activity" dashboard widget. */
    private List<com.finance.dto.response.FinancialRecordResponse> buildRecentActivity() {
        return recordRepository.findRecentActivity(PageRequest.of(0, 10))
                .stream()
                .map(recordService::toResponse)
                .collect(Collectors.toList());
    }
}
