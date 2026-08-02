package com.asad.expensetracker.dto.expense;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        String title,
        BigDecimal amount,
        String currency,
        String kind,
        String notes,
        LocalDate date,
        Long categoryId,
        String category,
        String categoryColor
) {
}
