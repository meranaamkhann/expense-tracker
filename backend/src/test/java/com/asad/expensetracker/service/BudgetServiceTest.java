package com.asad.expensetracker.service;

import com.asad.expensetracker.dto.budget.BudgetRequest;
import com.asad.expensetracker.exception.DuplicateResourceException;
import com.asad.expensetracker.model.Budget;
import com.asad.expensetracker.model.Category;
import com.asad.expensetracker.model.Expense;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.BudgetRepository;
import com.asad.expensetracker.repository.CategoryRepository;
import com.asad.expensetracker.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock private BudgetRepository budgetRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private ExpenseRepository expenseRepository;

    @InjectMocks
    private BudgetService budgetService;

    @Test
    void createRejectsSecondBudgetForSameCategory() {
        User user = User.builder().id(1L).build();
        Category category = Category.builder().id(2L).name("Food").user(user).build();
        when(categoryRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryId(1L, 2L))
                .thenReturn(Optional.of(Budget.builder().id(9L).build()));

        assertThatThrownBy(() -> budgetService.create(1L, user, new BudgetRequest(2L, BigDecimal.valueOf(5000))))
                .isInstanceOf(DuplicateResourceException.class);

        verify(budgetRepository, never()).save(any());
    }

    @Test
    void computesSpendAndPercentUsedForCurrentMonth() {
        User user = User.builder().id(1L).build();
        Category category = Category.builder().id(2L).name("Food").color("#f59e0b").user(user).build();
        when(categoryRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(category));
        when(budgetRepository.findByUserIdAndCategoryId(1L, 2L)).thenReturn(Optional.empty());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(inv -> {
            Budget b = inv.getArgument(0);
            b.setId(10L);
            return b;
        });
        when(expenseRepository.sumAmountByCategoryBetween(
                eq(1L), eq(2L), eq(Expense.TransactionKind.EXPENSE), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.valueOf(2500));

        var response = budgetService.create(1L, user, new BudgetRequest(2L, BigDecimal.valueOf(5000)));

        assertThat(response.monthlyLimit()).isEqualByComparingTo("5000");
        assertThat(response.spentThisMonth()).isEqualByComparingTo("2500");
        assertThat(response.remaining()).isEqualByComparingTo("2500");
        assertThat(response.percentUsed()).isEqualTo(50);
    }
}
