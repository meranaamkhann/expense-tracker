package com.asad.expensetracker.dto.auth;

import com.asad.expensetracker.dto.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        UserResponse user
) {
}
