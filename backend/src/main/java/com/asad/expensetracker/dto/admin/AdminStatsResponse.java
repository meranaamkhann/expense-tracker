package com.asad.expensetracker.dto.admin;

import java.math.BigDecimal;

public record AdminStatsResponse(
        long totalUsers,
        long totalExpenseEntries,
        BigDecimal totalExpenseVolume,
        BigDecimal totalIncomeVolume
) {
}
