package com.asad.expensetracker.service;

import com.asad.expensetracker.dto.category.CategoryRequest;
import com.asad.expensetracker.dto.category.CategoryResponse;
import com.asad.expensetracker.exception.BadRequestException;
import com.asad.expensetracker.exception.DuplicateResourceException;
import com.asad.expensetracker.exception.ResourceNotFoundException;
import com.asad.expensetracker.mapper.Mappers;
import com.asad.expensetracker.model.Category;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.CategoryRepository;
import com.asad.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    /** Seeded for every new user on registration so the app is usable immediately. */
    public static final List<Category> DEFAULTS = List.of(
            Category.builder().name("Food & Drinks").color("#f59e0b").icon("Utensils").isDefault(true).build(),
            Category.builder().name("Shopping").color("#ec4899").icon("ShoppingBag").isDefault(true).build(),
            Category.builder().name("Housing").color("#3b82f6").icon("Home").isDefault(true).build(),
            Category.builder().name("Transportation").color("#10b981").icon("Car").isDefault(true).build(),
            Category.builder().name("Entertainment").color("#8b5cf6").icon("Tv").isDefault(true).build(),
            Category.builder().name("Salary").color("#22c55e").icon("IndianRupee").isDefault(true).build()
    );

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional
    public void seedDefaultCategories(User user) {
        for (Category template : DEFAULTS) {
            categoryRepository.save(Category.builder()
                    .name(template.getName())
                    .color(template.getColor())
                    .icon(template.getIcon())
                    .isDefault(true)
                    .user(user)
                    .build());
        }
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(Long userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(Mappers::toCategoryResponse)
                .toList();
    }

    @Transactional
    public CategoryResponse create(Long userId, User user, CategoryRequest request) {
        if (categoryRepository.existsByUserIdAndNameIgnoreCase(userId, request.name().trim())) {
            throw new DuplicateResourceException("A category named '" + request.name() + "' already exists");
        }
        Category category = Category.builder()
                .name(request.name().trim())
                .color(request.color())
                .icon(request.icon())
                .isDefault(false)
                .user(user)
                .build();
        return Mappers.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long userId, Long categoryId, CategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + categoryId));

        boolean nameChanged = !category.getName().equalsIgnoreCase(request.name().trim());
        if (nameChanged && categoryRepository.existsByUserIdAndNameIgnoreCase(userId, request.name().trim())) {
            throw new DuplicateResourceException("A category named '" + request.name() + "' already exists");
        }

        category.setName(request.name().trim());
        if (request.color() != null) category.setColor(request.color());
        if (request.icon() != null) category.setIcon(request.icon());
        return Mappers.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long userId, Long categoryId) {
        Category category = categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + categoryId));

        if (categoryRepository.countByUserId(userId) <= 1) {
            throw new BadRequestException("You must keep at least one category");
        }
        if (expenseRepository.countByCategoryId(categoryId) > 0) {
            throw new BadRequestException("Cannot delete a category that has expenses linked to it. Reassign or delete those expenses first.");
        }
        categoryRepository.delete(category);
    }
}
