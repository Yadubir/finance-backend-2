package com.finance.dto.response;

import com.finance.model.RecordType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialRecordResponse {
    private Long        id;
    private BigDecimal  amount;
    private RecordType  type;
    private String      category;
    private LocalDate   recordDate;
    private String      description;
    private String      createdByName;  // resolved from the User FK — avoids exposing internal IDs
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
