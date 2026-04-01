package com.finance.controller;

import com.finance.dto.request.CreateRecordRequest;
import com.finance.dto.request.UpdateRecordRequest;
import com.finance.dto.response.FinancialRecordResponse;
import com.finance.dto.response.PagedResponse;
import com.finance.model.RecordType;
import com.finance.service.FinancialRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Financial records endpoints.
 *
 * URL design:
 *   GET    /api/records           → list with filters + pagination  (ALL roles)
 *   POST   /api/records           → create                          (ADMIN)
 *   GET    /api/records/{id}      → get single record               (ALL roles)
 *   PATCH  /api/records/{id}      → partial update                  (ADMIN)
 *   DELETE /api/records/{id}      → soft delete                     (ADMIN)
 *
 * Filtering is done via query params rather than request body
 * because GET requests should be idempotent and bookmarkable.
 */
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Tag(name = "Financial Records", description = "CRUD operations for financial entries")
@SecurityRequirement(name = "bearerAuth")
public class FinancialRecordController {

    private final FinancialRecordService recordService;

    @GetMapping
    @Operation(
        summary = "List financial records (paginated + filtered)",
        description = "All authenticated users can access. Supports filtering by type, category, and date range."
    )
    public ResponseEntity<PagedResponse<FinancialRecordResponse>> getRecords(
            @Parameter(description = "Filter by INCOME or EXPENSE")
            @RequestParam(required = false) RecordType type,

            @Parameter(description = "Filter by category name (case-insensitive)")
            @RequestParam(required = false) String category,

            @Parameter(description = "Start date inclusive, format: yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @Parameter(description = "End date inclusive, format: yyyy-MM-dd")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(recordService.getRecords(type, category, from, to, page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single financial record by ID")
    @ApiResponse(responseCode = "404", description = "Record not found or soft-deleted")
    public ResponseEntity<FinancialRecordResponse> getRecordById(@PathVariable Long id) {
        return ResponseEntity.ok(recordService.getRecordById(id));
    }

    @PostMapping
    @Operation(summary = "Create a financial record", description = "Admin only.")
    @ApiResponse(responseCode = "201", description = "Record created successfully")
    public ResponseEntity<FinancialRecordResponse> createRecord(
            @Valid @RequestBody CreateRecordRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(recordService.createRecord(request));
    }

    @PatchMapping("/{id}")
    @Operation(
        summary = "Partially update a financial record",
        description = "Admin only. Only provided fields are updated."
    )
    public ResponseEntity<FinancialRecordResponse> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRecordRequest request
    ) {
        return ResponseEntity.ok(recordService.updateRecord(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a financial record", description = "Admin only.")
    @ApiResponse(responseCode = "204", description = "Record deleted (soft)")
    public ResponseEntity<Void> deleteRecord(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return ResponseEntity.noContent().build();
    }
}
