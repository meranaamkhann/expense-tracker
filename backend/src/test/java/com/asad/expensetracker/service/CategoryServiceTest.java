package com.asad.expensetracker.service;

import com.asad.expensetracker.dto.category.CategoryRequest;
import com.asad.expensetracker.exception.BadRequestException;
import com.asad.expensetracker.exception.DuplicateResourceException;
import com.asad.expensetracker.exception.ResourceNotFoundException;
import com.asad.expensetracker.model.Category;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.CategoryRepository;
import com.asad.expensetracker.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ExpenseRepository expenseRepository;

    @InjectMocks
    private CategoryService categoryService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Asad").email("asad@example.com").build();
    }

    @Test
    void createRejectsDuplicateNameForSameUser() {
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(1L, "Groceries")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.create(1L, user, new CategoryRequest("Groceries", "#fff", null)))
                .isInstanceOf(DuplicateResourceException.class);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createSavesCategoryWhenNameIsUnique() {
        when(categoryRepository.existsByUserIdAndNameIgnoreCase(1L, "Travel")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        var response = categoryService.create(1L, user, new CategoryRequest("Travel", "#123456", "Plane"));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Travel");
    }

    @Test
    void deleteFailsWhenItIsTheOnlyCategoryLeft() {
        Category category = Category.builder().id(5L).name("Only One").user(user).build();
        when(categoryRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(category));
        when(categoryRepository.countByUserId(1L)).thenReturn(1L);

        assertThatThrownBy(() -> categoryService.delete(1L, 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least one category");
    }

    @Test
    void deleteFailsWhenCategoryHasExpenses() {
        Category category = Category.builder().id(5L).name("Groceries").user(user).build();
        when(categoryRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(category));
        when(categoryRepository.countByUserId(1L)).thenReturn(3L);
        when(expenseRepository.countByCategoryId(5L)).thenReturn(2L);

        assertThatThrownBy(() -> categoryService.delete(1L, 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete");

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteThrowsNotFoundForMissingCategory() {
        when(categoryRepository.findByIdAndUserId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.delete(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
