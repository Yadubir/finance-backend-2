package com.finance.dto.response;

import com.finance.model.Role;
import com.finance.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Safe user projection — password hash is never included.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long          id;
    private String        email;
    private String        fullName;
    private Role          role;
    private UserStatus    status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
