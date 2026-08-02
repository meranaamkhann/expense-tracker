package com.asad.expensetracker.controller;

import com.asad.expensetracker.dto.expense.ExpenseRequest;
import com.asad.expensetracker.dto.expense.ExpenseResponse;
import com.asad.expensetracker.security.UserPrincipal;
import com.asad.expensetracker.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(expenseService.getAll(principal.getId()));
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@AuthenticationPrincipal UserPrincipal principal,
                                                    @Valid @RequestBody ExpenseRequest request) {
        ExpenseResponse created = expenseService.create(principal.getId(), principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> update(@AuthenticationPrincipal UserPrincipal principal,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody ExpenseRequest request) {
        return ResponseEntity.ok(expenseService.update(principal.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        expenseService.delete(principal.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
