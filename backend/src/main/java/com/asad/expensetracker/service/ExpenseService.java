package com.asad.expensetracker.service;

import com.asad.expensetracker.dto.expense.ExpenseRequest;
import com.asad.expensetracker.dto.expense.ExpenseResponse;
import com.asad.expensetracker.exception.BadRequestException;
import com.asad.expensetracker.exception.ResourceNotFoundException;
import com.asad.expensetracker.mapper.Mappers;
import com.asad.expensetracker.model.Category;
import com.asad.expensetracker.model.Expense;
import com.asad.expensetracker.model.User;
import com.asad.expensetracker.repository.CategoryRepository;
import com.asad.expensetracker.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ExpenseRepository expenseRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public List<ExpenseResponse> getAll(Long userId) {
        return expenseRepository.findByUserIdOrderByExpenseDateDesc(userId).stream()
                .map(Mappers::toExpenseResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getPage(Long userId, int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "expenseDate"));
        return expenseRepository.findByUserIdOrderByExpenseDateDesc(userId, pageable)
                .map(Mappers::toExpenseResponse);
    }

    @Transactional
    public ExpenseResponse create(Long userId, User user, ExpenseRequest request) {
        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + request.categoryId()));

        Expense.TransactionKind kind = parseKind(request.kind());

        Expense expense = Expense.builder()
                .title(request.title().trim())
                .amount(request.amount())
                .currency(request.currency() == null || request.currency().isBlank() ? "INR" : request.currency().toUpperCase())
                .kind(kind)
                .notes(request.notes())
                .expenseDate(request.date())
                .category(category)
                .user(user)
                .build();

        return Mappers.toExpenseResponse(expenseRepository.save(expense));
    }

    @Transactional
    public ExpenseResponse update(Long userId, Long expenseId, ExpenseRequest request) {
        Expense expense = expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id " + expenseId));

        Category category = categoryRepository.findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id " + request.categoryId()));

        expense.setTitle(request.title().trim());
        expense.setAmount(request.amount());
        expense.setCategory(category);
        expense.setExpenseDate(request.date());
        expense.setKind(parseKind(request.kind()));
        expense.setNotes(request.notes());
        if (request.currency() != null && !request.currency().isBlank()) {
            expense.setCurrency(request.currency().toUpperCase());
        }

        return Mappers.toExpenseResponse(expenseRepository.save(expense));
    }

    @Transactional
    public void delete(Long userId, Long expenseId) {
        expenseRepository.findByIdAndUserId(expenseId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found with id " + expenseId));
        expenseRepository.deleteByIdAndUserId(expenseId, userId);
    }

    private Expense.TransactionKind parseKind(String raw) {
        if (raw == null || raw.isBlank()) return Expense.TransactionKind.EXPENSE;
        try {
            return Expense.TransactionKind.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("kind must be 'expense' or 'income'");
        }
    }
}
