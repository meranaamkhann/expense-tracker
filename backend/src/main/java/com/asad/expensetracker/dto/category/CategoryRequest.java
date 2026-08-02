package com.asad.expensetracker.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 20) String color,
        @Size(max = 40) String icon
) {
}
