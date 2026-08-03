package com.asad.expensetracker.repository;

import com.asad.expensetracker.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    Optional<Budget> findByIdAndUserId(Long id, Long userId);
    Optional<Budget> findByUserIdAndCategoryId(Long userId, Long categoryId);
    void deleteByIdAndUserId(Long id, Long userId);
}
