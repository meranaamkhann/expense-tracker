package com.asad.expensetracker.dto.user;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequest(
        @NotBlank(message = "Password confirmation is required") String password
) {
}
