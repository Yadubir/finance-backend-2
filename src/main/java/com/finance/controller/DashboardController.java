package com.finance.controller;

import com.finance.dto.response.DashboardSummaryResponse;
import com.finance.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Dashboard analytics endpoints.
 *
 * URL design:
 *   GET /api/dashboard/summary       → full summary (ANALYST, ADMIN)
 *   GET /api/dashboard/basic         → totals only  (ALL roles including VIEWER)
 *
 * Both endpoints accept optional date range query params.
 * If omitted, the full date range is used (no lower/upper bound).
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Analytics and summary endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(
        summary = "Full dashboard summary",
        description = """
            Returns total income, expenses, net balance, category breakdowns,
            monthly trends, and recent activity. Accessible by ANALYST and ADMIN.
            """
    )
    public ResponseEntity<DashboardSummaryResponse> getFullSummary(
            @Parameter(description = "Period start date (yyyy-MM-dd), optional")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "Period end date (yyyy-MM-dd), optional")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(dashboardService.getFullSummary(from, to));
    }

    @GetMapping("/basic")
    @Operation(
        summary = "Basic summary (totals only)",
        description = "Returns only totalIncome, totalExpenses, and netBalance. Accessible by all roles."
    )
    public ResponseEntity<Map<String, Object>> getBasicSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(dashboardService.getBasicSummary(from, to));
    }
}
