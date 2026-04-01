package com.finance.dto.request;

import com.finance.model.Role;
import com.finance.model.UserStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * DTO for creating a new user. Kept separate from UpdateUserRequest
 * because creation requires a password while updates do not.
 */
@Data
public class CreateUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit"
    )
    private String password;

    @NotNull(message = "Role is required")
    private Role role;
}
