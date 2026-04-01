package com.finance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Finance Dashboard Backend — entry point.
 *
 * Key design choices:
 * - @EnableJpaAuditing: auto-populates createdAt/updatedAt via @CreatedDate / @LastModifiedDate
 * - Single module, layered architecture (Controller → Service → Repository)
 */
@SpringBootApplication
@EnableJpaAuditing
public class FinanceBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FinanceBackendApplication.class, args);
    }
}
