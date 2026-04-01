package com.finance.config;

import com.finance.model.*;
import com.finance.repository.FinancialRecordRepository;
import com.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DataSeeder — runs once on startup via CommandLineRunner.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final UserRepository           userRepository;
    private final FinancialRecordRepository recordRepository;
    private final PasswordEncoder          passwordEncoder;

    @Bean
    public CommandLineRunner seedDatabase() {
        return args -> {
            seedUsers();
            seedRecords();
        };
    }

    private void seedUsers() {
        // Idempotency check — only seed if no users exist yet
        if (userRepository.count() > 0) {
            log.info("Users already seeded, skipping.");
            return;
        }

        log.info("Seeding default users...");

        User admin = User.builder()
                .email("admin@finance.com")
                .passwordHash(passwordEncoder.encode("Admin@123"))
                .fullName("System Admin")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        User analyst = User.builder()
                .email("analyst@finance.com")
                .passwordHash(passwordEncoder.encode("Analyst@123"))
                .fullName("Jane Analyst")
                .role(Role.ANALYST)
                .status(UserStatus.ACTIVE)
                .build();

        User viewer = User.builder()
                .email("viewer@finance.com")
                .passwordHash(passwordEncoder.encode("Viewer@123"))
                .fullName("Bob Viewer")
                .role(Role.VIEWER)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.saveAll(List.of(admin, analyst, viewer));
        log.info("Seeded 3 users: admin@finance.com / analyst@finance.com / viewer@finance.com");
    }

    private void seedRecords() {
        if (recordRepository.count() > 0) {
            log.info("Records already seeded, skipping.");
            return;
        }

        // Fetch the admin user to associate as record creator
        User admin = userRepository.findActiveByEmail("admin@finance.com")
                .orElseThrow(() -> new IllegalStateException("Admin user not found during seeding"));

        log.info("Seeding sample financial records...");

        List<FinancialRecord> records = List.of(
                record(50000.00, RecordType.INCOME,  "Salary",     "2024-01-31", "January salary",         admin),
                record( 1200.00, RecordType.EXPENSE, "Rent",       "2024-02-01", "Monthly office rent",    admin),
                record(30000.00, RecordType.INCOME,  "Freelance",  "2024-02-15", "Consulting project fee", admin),
                record(   450.00,RecordType.EXPENSE, "Utilities",  "2024-02-20", "Electricity and water",  admin),
                record(50000.00, RecordType.INCOME,  "Salary",     "2024-02-28", "February salary",        admin),
                record( 8000.00, RecordType.EXPENSE, "Marketing",  "2024-03-05", "Ad campaign spend",      admin),
                record(50000.00, RecordType.INCOME,  "Salary",     "2024-03-31", "March salary",           admin),
                record( 1200.00, RecordType.EXPENSE, "Rent",       "2024-03-01", "Monthly office rent",    admin),
                record(15000.00, RecordType.INCOME,  "Investment", "2024-03-20", "Dividend payout",        admin),
                record( 3500.00, RecordType.EXPENSE, "Software",   "2024-03-25", "Annual SaaS licenses",   admin)
        );

        recordRepository.saveAll(records);
        log.info("Seeded {} financial records.", records.size());
    }

    private FinancialRecord record(
            double amount, RecordType type, String category,
            String date, String description, User createdBy
    ) {
        return FinancialRecord.builder()
                .amount(BigDecimal.valueOf(amount))
                .type(type)
                .category(category)
                .recordDate(LocalDate.parse(date))
                .description(description)
                .createdBy(createdBy)
                .build();
    }
}
