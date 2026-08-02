package com.asad.expensetracker.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "token is required") String token,
        @NotBlank @Size(min = 8, max = 72) String newPassword
) {
}
