package com.asad.expensetracker.dto.analytics;

import java.math.BigDecimal;

public record CategoryTotal(
        Long categoryId,
        String category,
        String color,
        BigDecimal total
) {
}
