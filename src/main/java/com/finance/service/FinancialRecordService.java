package com.finance.service;

import com.finance.dto.request.CreateRecordRequest;
import com.finance.dto.request.UpdateRecordRequest;
import com.finance.dto.response.FinancialRecordResponse;
import com.finance.dto.response.PagedResponse;
import com.finance.exception.ResourceNotFoundException;
import com.finance.model.FinancialRecord;
import com.finance.model.RecordType;
import com.finance.model.User;
import com.finance.repository.FinancialRecordRepository;
import com.finance.repository.UserRepository;
import com.finance.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Financial records service.
 *
 * Role-based access:
 *   READ  (list, get)   → VIEWER, ANALYST, ADMIN
 *   WRITE (create, update, delete) → ADMIN only
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialRecordService {

    private final FinancialRecordRepository recordRepository;
    private final UserRepository            userRepository;

    // ── Queries ───────────────────────────────────────────────────────────────

    /**
     * Paginated, filtered list of financial records.
     * All authenticated roles can access this.
     *
     * @param type     filter by INCOME or EXPENSE (optional)
     * @param category filter by category name, case-insensitive (optional)
     * @param from     start date inclusive (optional)
     * @param to       end date inclusive (optional)
     * @param page     0-indexed page
     * @param size     page size (capped at 100)
     */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Transactional(readOnly = true)
    public PagedResponse<FinancialRecordResponse> getRecords(
            RecordType type, String category,
            LocalDate from, LocalDate to,
            int page, int size
    ) {
        size = Math.min(size, 100);
        var pageable = PageRequest.of(page, size, Sort.by("recordDate").descending());
        return PagedResponse.from(
                recordRepository.findWithFilters(type, category, from, to, pageable)
                        .map(this::toResponse)
        );
    }

    /** Retrieve a single active record by ID. All roles. */
    @PreAuthorize("hasAnyRole('VIEWER', 'ANALYST', 'ADMIN')")
    @Transactional(readOnly = true)
    public FinancialRecordResponse getRecordById(Long id) {
        return toResponse(findActiveRecordOrThrow(id));
    }

    // Mutations 

    /** Create a new financial record. Admin only. */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public FinancialRecordResponse createRecord(CreateRecordRequest request) {
        User currentUser = getCurrentUser();

        FinancialRecord record = FinancialRecord.builder()
                .amount(request.getAmount())
                .type(request.getType())
                .category(request.getCategory().trim())
                .recordDate(request.getRecordDate())
                .description(request.getDescription())
                .createdBy(currentUser)
                .build();

        FinancialRecord saved = recordRepository.save(record);
        log.info("Record created: id={} type={} amount={} by userId={}",
                saved.getId(), saved.getType(), saved.getAmount(), currentUser.getId());
        return toResponse(saved);
    }

    /**
     * Partial update — only fields present in the request body are applied.
     * Admin only.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public FinancialRecordResponse updateRecord(Long id, UpdateRecordRequest request) {
        FinancialRecord record = findActiveRecordOrThrow(id);

        if (request.getAmount()      != null) record.setAmount(request.getAmount());
        if (request.getType()        != null) record.setType(request.getType());
        if (request.getCategory()    != null) record.setCategory(request.getCategory().trim());
        if (request.getRecordDate()  != null) record.setRecordDate(request.getRecordDate());
        if (request.getDescription() != null) record.setDescription(request.getDescription());

        log.info("Record updated: id={} by userId={}", id, getCurrentPrincipal().getId());
        return toResponse(recordRepository.save(record));
    }

    /**
     * Soft-delete a record. Admin only.
     * Sets deletedAt — the record is excluded from all active queries
     * but remains in the database for audit purposes.
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public void deleteRecord(Long id) {
        FinancialRecord record = findActiveRecordOrThrow(id);
        record.setDeletedAt(LocalDateTime.now());
        recordRepository.save(record);
        log.info("Record soft-deleted: id={} by userId={}", id, getCurrentPrincipal().getId());
    }

    // Helpers

    private FinancialRecord findActiveRecordOrThrow(Long id) {
        return recordRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FinancialRecord", id));
    }

    private User getCurrentUser() {
        UserPrincipal principal = getCurrentPrincipal();
        return userRepository.findActiveById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    private UserPrincipal getCurrentPrincipal() {
        return (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
    }

    public FinancialRecordResponse toResponse(FinancialRecord r) {
        return FinancialRecordResponse.builder()
                .id(r.getId())
                .amount(r.getAmount())
                .type(r.getType())
                .category(r.getCategory())
                .recordDate(r.getRecordDate())
                .description(r.getDescription())
                .createdByName(r.getCreatedBy() != null ? r.getCreatedBy().getFullName() : "Unknown")
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
