package com.finance.dto.request;

import com.finance.model.Role;
import com.finance.model.UserStatus;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Partial update DTO for users. All fields are optional.
 * Only non-null fields will be applied .
 */
@Data
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    private Role role;

    private UserStatus status;
}
