package com.finance.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Top-level dashboard summary payload.
 * Designed to power a finance dashboard with a single API call
 * rather than N separate calls — reduces frontend round-trips.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;          // totalIncome - totalExpenses

    private List<CategoryTotal>  incomeByCateogry;
    private List<CategoryTotal>  expensesByCategory;
    private List<MonthlyTrend>   monthlyTrends;
    private List<FinancialRecordResponse> recentActivity;  // last 10 records

    // ── Nested projection types

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryTotal {
        private String     category;
        private BigDecimal total;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTrend {
        private int        year;
        private int        month;
        private String     monthLabel;   // e.g. "Jan 2024" — formatted server-side for convenience
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal net;
    }
}
