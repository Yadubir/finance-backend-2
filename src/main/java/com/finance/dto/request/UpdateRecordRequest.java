package com.finance.dto.request;

import com.finance.model.RecordType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Partial update DTO for financial records (PATCH semantics).
 * All fields are optional — only non-null fields are applied.
 */
@Data
public class UpdateRecordRequest {

    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 15, fraction = 4, message = "Amount format is invalid")
    private BigDecimal amount;

    private RecordType type;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    @PastOrPresent(message = "Record date cannot be in the future")
    private LocalDate recordDate;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;
}
