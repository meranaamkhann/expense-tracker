package com.asad.expensetracker.dto.analytics;

import java.math.BigDecimal;
import java.util.List;

public record AnalyticsSummaryResponse(
        String currency,
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal balance,
        List<CategoryTotal> topCategories,
        List<MonthlyTotal> monthly
) {
}
