package com.asad.expensetracker.dto.category;

public record CategoryResponse(
        Long id,
        String name,
        String color,
        String icon,
        boolean isDefault
) {
}
