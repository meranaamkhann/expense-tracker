package com.asad.expensetracker.controller;

import com.asad.expensetracker.dto.category.CategoryRequest;
import com.asad.expensetracker.dto.category.CategoryResponse;
import com.asad.expensetracker.security.UserPrincipal;
import com.asad.expensetracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(categoryService.getAll(principal.getId()));
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                     @Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.create(principal.getId(), principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.update(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        categoryService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
