package com.asad.expensetracker.service;

import com.asad.expensetracker.dto.budget.BudgetRequest;
import com.asad.expensetracker.dto.budget.BudgetResponse;
import com.asad.expensetracker.exception.DuplicateResourceException;
import com.asad.expensetracker.exception.ResourceNotFoundException;
import com.asad.expensetracker.model.Budget;
import com.asad.expensetracker.model.Category;
import com.asad.expensetracker.model.Expense;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.BudgetRepository;
import com.asad.expensetracker.repository.CategoryRepository;
import com.asad.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional(readOnly = true)
    public List<BudgetResponse> getAll(Long userId) {
        return budgetRepository.findByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BudgetResponse create(Long userId, User user, BudgetRequest request) {
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + request.categoryId()));

        if (budgetRepository.findByUserIdAndCategoryId(userId, request.categoryId()).isPresent()) {
            throw new DuplicateResourceException("A budget already exists for " + category.getName() + " — edit it instead.");
        }

        Budget budget = Budget.builder()
                .user(user)
                .category(category)
                .monthlyLimit(request.monthlyLimit())
                .build();

        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public BudgetResponse update(Long userId, Long budgetId, BudgetRequest request) {
        Budget budget = budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + budgetId));

        if (!budget.getCategory().getId().equals(request.categoryId())) {
            Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + request.categoryId()));
            budgetRepository.findByUserIdAndCategoryId(userId, request.categoryId())
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException("A budget already exists for " + category.getName());
                    });
            budget.setCategory(category);
        }

        budget.setMonthlyLimit(request.monthlyLimit());
        return toResponse(budgetRepository.save(budget));
    }

    @Transactional
    public void delete(Long userId, Long budgetId) {
        budgetRepository.findByIdAndUserId(budgetId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget not found with id " + budgetId));
        budgetRepository.deleteByIdAndUserId(budgetId, userId);
    }

    private BudgetResponse toResponse(Budget budget) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate from = currentMonth.atDay(1);
        LocalDate to = currentMonth.atEndOfMonth();

        BigDecimal spent = expenseRepository.sumAmountByCategoryBetween(
                budget.getUser().getId(), budget.getCategory().getId(), Expense.TransactionKind.EXPENSE, from, to);
        BigDecimal remaining = budget.getMonthlyLimit().subtract(spent);

        int percentUsed = budget.getMonthlyLimit().compareTo(BigDecimal.ZERO) == 0
                ? 0
                : spent.multiply(BigDecimal.valueOf(100))
                        .divide(budget.getMonthlyLimit(), 0, RoundingMode.HALF_UP)
                        .intValue();

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getCategory().getColor(),
                budget.getMonthlyLimit(),
                spent,
                remaining,
                percentUsed
        );
    }
}
