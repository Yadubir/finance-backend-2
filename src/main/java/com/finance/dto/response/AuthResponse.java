package com.finance.dto.response;

import com.finance.model.Role;
import com.finance.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Returned after successful login.
 * Never exposes the password hash.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;   // always "Bearer"
    private Long   userId;
    private String email;
    private String fullName;
    private Role   role;
}
