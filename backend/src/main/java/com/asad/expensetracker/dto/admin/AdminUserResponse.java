package com.asad.expensetracker.dto.admin;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean enabled,
        boolean emailVerified,
        Instant createdAt
) {
}
