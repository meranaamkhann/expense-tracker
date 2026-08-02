package com.asad.expensetracker.dto.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpenseRequest(
        @NotBlank @Size(max = 160) String title,
        @NotNull @PositiveOrZero BigDecimal amount,
        @NotNull Long categoryId,
        @NotNull LocalDate date,
        String kind,
        @Size(max = 500) String notes,
        @Size(max = 3) String currency
) {
}
