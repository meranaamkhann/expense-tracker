package com.asad.expensetracker.dto.analytics;

import java.math.BigDecimal;

public record MonthlyTotal(
        String month,
        BigDecimal income,
        BigDecimal expense
) {
}
