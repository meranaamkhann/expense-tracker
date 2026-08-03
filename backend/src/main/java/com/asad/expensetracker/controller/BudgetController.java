package com.asad.expensetracker.controller;

import com.asad.expensetracker.dto.budget.BudgetRequest;
import com.asad.expensetracker.dto.budget.BudgetResponse;
import com.asad.expensetracker.security.UserPrincipal;
import com.asad.expensetracker.service.BudgetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<List<BudgetResponse>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(budgetService.getAll(principal.getId()));
    }

    @PostMapping
    public ResponseEntity<BudgetResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody BudgetRequest request) {
        BudgetResponse created = budgetService.create(principal.getId(), principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BudgetResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody BudgetRequest request) {
        return ResponseEntity.ok(budgetService.update(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        budgetService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
