package com.asad.expensetracker.service;

import com.asad.expensetracker.dto.analytics.AnalyticsSummaryResponse;
import com.asad.expensetracker.dto.analytics.CategoryTotal;
import com.asad.expensetracker.dto.analytics.MonthlyTotal;
import com.asad.expensetracker.model.Expense;
import com.asad.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ExpenseRepository expenseRepository;

    /**
     * Dashboard summary for a rolling window (defaults to the last 6 months, capped at 5 years
     * so a runaway "months" param from a client can't trigger an expensive scan).
     */
    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getSummary(Long userId, int months) {
        int safeMonths = Math.min(Math.max(months, 1), 60);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusMonths(safeMonths).withDayOfMonth(1);

        BigDecimal totalIncome = expenseRepository.sumAmountBetween(userId, Expense.TransactionKind.INCOME, from, to);
        BigDecimal totalExpense = expenseRepository.sumAmountBetween(userId, Expense.TransactionKind.EXPENSE, from, to);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        List<CategoryTotal> topCategories = expenseRepository
                .sumByCategoryBetween(userId, Expense.TransactionKind.EXPENSE, from, to)
                .stream()
                .map(row -> new CategoryTotal((Long) row[0], (String) row[1], (String) row[2], (BigDecimal) row[3]))
                .limit(8)
                .toList();

        List<MonthlyTotal> monthly = new ArrayList<>();
        YearMonth cursor = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);
        while (!cursor.isAfter(end)) {
            LocalDate monthStart = cursor.atDay(1);
            LocalDate monthEnd = cursor.atEndOfMonth();
            BigDecimal income = expenseRepository.sumAmountBetween(userId, Expense.TransactionKind.INCOME, monthStart, monthEnd);
            BigDecimal expense = expenseRepository.sumAmountBetween(userId, Expense.TransactionKind.EXPENSE, monthStart, monthEnd);
            String label = cursor.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + cursor.getYear();
            monthly.add(new MonthlyTotal(label, income, expense));
            cursor = cursor.plusMonths(1);
        }

        return new AnalyticsSummaryResponse("INR", totalIncome, totalExpense, balance, topCategories, monthly);
    }
}
