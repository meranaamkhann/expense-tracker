package com.asad.expensetracker.mapper;

import com.asad.expensetracker.dto.category.CategoryResponse;
import com.asad.expensetracker.dto.expense.ExpenseResponse;
import com.asad.expensetracker.dto.user.UserResponse;
import com.asad.expensetracker.model.Category;
import com.asad.expensetracker.model.Expense;
import com.asad.expensetracker.model.User;

/** Central place that turns entities into API-facing DTOs so we never leak JPA entities over REST. */
public final class Mappers {

    private Mappers() {
    }

    public static UserResponse toUserResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole().name(), user.isEmailVerified());
    }

    public static CategoryResponse toCategoryResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getIcon(),
                category.isDefault()
        );
    }

    public static ExpenseResponse toExpenseResponse(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getCurrency(),
                expense.getKind().name().toLowerCase(),
                expense.getNotes(),
                expense.getExpenseDate(),
                expense.getCategory().getId(),
                expense.getCategory().getName(),
                expense.getCategory().getColor()
        );
    }
}
