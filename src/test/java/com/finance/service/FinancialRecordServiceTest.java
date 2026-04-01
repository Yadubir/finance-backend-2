package com.finance.service;

import com.finance.dto.request.CreateRecordRequest;
import com.finance.dto.request.UpdateRecordRequest;
import com.finance.dto.response.FinancialRecordResponse;
import com.finance.dto.response.PagedResponse;
import com.finance.exception.ResourceNotFoundException;
import com.finance.model.*;
import com.finance.repository.FinancialRecordRepository;
import com.finance.repository.UserRepository;
import com.finance.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FinancialRecordService Tests")
class FinancialRecordServiceTest {

    @Mock FinancialRecordRepository recordRepository;
    @Mock UserRepository            userRepository;

    @InjectMocks FinancialRecordService recordService;

    private User            adminUser;
    private FinancialRecord sampleRecord;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L).email("admin@finance.com").fullName("Admin")
                .role(Role.ADMIN).status(UserStatus.ACTIVE).build();

        sampleRecord = FinancialRecord.builder()
                .id(1L)
                .amount(new BigDecimal("5000.00"))
                .type(RecordType.INCOME)
                .category("Salary")
                .recordDate(LocalDate.of(2024, 3, 31))
                .description("March salary")
                .createdBy(adminUser)
                .build();

        mockSecurityContext(adminUser);
    }

    // ── getRecords ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRecords() returns paginated response with correct mapping")
    void getRecords_returnsPagedResponse() {
        Page<FinancialRecord> page = new PageImpl<>(List.of(sampleRecord));
        when(recordRepository.findWithFilters(any(), any(), any(), any(), any())).thenReturn(page);

        PagedResponse<FinancialRecordResponse> result =
                recordService.getRecords(null, null, null, null, 0, 20);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCategory()).isEqualTo("Salary");
        assertThat(result.getContent().get(0).getAmount()).isEqualByComparingTo("5000.00");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // ── getRecordById ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("getRecordById() returns record when found")
    void getRecordById_existing_returnsResponse() {
        when(recordRepository.findActiveById(1L)).thenReturn(Optional.of(sampleRecord));

        FinancialRecordResponse response = recordService.getRecordById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getType()).isEqualTo(RecordType.INCOME);
        assertThat(response.getCreatedByName()).isEqualTo("Admin");
    }

    @Test
    @DisplayName("getRecordById() throws ResourceNotFoundException when not found")
    void getRecordById_missing_throwsNotFoundException() {
        when(recordRepository.findActiveById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.getRecordById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── createRecord ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRecord() persists and returns a new record")
    void createRecord_validRequest_persistsRecord() {
        CreateRecordRequest req = new CreateRecordRequest();
        req.setAmount(new BigDecimal("2500.00"));
        req.setType(RecordType.EXPENSE);
        req.setCategory("Software");
        req.setRecordDate(LocalDate.of(2024, 4, 1));
        req.setDescription("IDE licenses");

        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(adminUser));
        when(recordRepository.save(any())).thenAnswer(inv -> {
            FinancialRecord r = inv.getArgument(0);
            r.setId(42L);
            return r;
        });

        FinancialRecordResponse response = recordService.createRecord(req);

        assertThat(response.getId()).isEqualTo(42L);
        assertThat(response.getCategory()).isEqualTo("Software");
        assertThat(response.getType()).isEqualTo(RecordType.EXPENSE);
        verify(recordRepository).save(any(FinancialRecord.class));
    }

    // ── updateRecord ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateRecord() applies only non-null fields (PATCH semantics)")
    void updateRecord_partialRequest_updatesOnlyProvidedFields() {
        UpdateRecordRequest req = new UpdateRecordRequest();
        req.setAmount(new BigDecimal("9999.00")); // only updating amount

        when(recordRepository.findActiveById(1L)).thenReturn(Optional.of(sampleRecord));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FinancialRecordResponse response = recordService.updateRecord(1L, req);

        assertThat(response.getAmount()).isEqualByComparingTo("9999.00");
        assertThat(response.getCategory()).isEqualTo("Salary");   // unchanged
        assertThat(response.getType()).isEqualTo(RecordType.INCOME); // unchanged
    }

    // ── deleteRecord ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteRecord() sets deletedAt on the record (soft delete)")
    void deleteRecord_existing_setsDeletedAt() {
        when(recordRepository.findActiveById(1L)).thenReturn(Optional.of(sampleRecord));
        when(recordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        recordService.deleteRecord(1L);

        verify(recordRepository).save(argThat(r -> r.getDeletedAt() != null));
    }

    @Test
    @DisplayName("deleteRecord() throws ResourceNotFoundException if record is already deleted")
    void deleteRecord_alreadyDeleted_throwsNotFoundException() {
        when(recordRepository.findActiveById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recordService.deleteRecord(77L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void mockSecurityContext(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth     = mock(Authentication.class);
        SecurityContext ctx     = mock(SecurityContext.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}
