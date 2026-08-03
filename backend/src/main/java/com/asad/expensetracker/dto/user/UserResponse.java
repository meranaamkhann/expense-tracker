package com.asad.expensetracker.dto.user;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean emailVerified
) {
}
