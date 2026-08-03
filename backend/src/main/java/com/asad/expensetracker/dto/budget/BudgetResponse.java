package com.asad.expensetracker.dto.budget;

import java.math.BigDecimal;

public record BudgetResponse(
        Long id,
        Long categoryId,
        String category,
        String categoryColor,
        BigDecimal monthlyLimit,
        BigDecimal spentThisMonth,
        BigDecimal remaining,
        int percentUsed
) {
}
