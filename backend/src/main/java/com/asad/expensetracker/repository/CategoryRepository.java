package com.asad.expensetracker.repository;

import com.asad.expensetracker.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIdOrderByNameAsc(Long userId);
    Optional<Category> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);
    long countByUserId(Long userId);
}
