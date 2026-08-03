package com.asad.expensetracker.repository;

import com.asad.expensetracker.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByUserIdOrderByExpenseDateDesc(Long userId, Pageable pageable);

    List<Expense> findByUserIdOrderByExpenseDateDesc(Long userId);

    Optional<Expense> findByIdAndUserId(Long id, Long userId);

    void deleteByIdAndUserId(Long id, Long userId);

    long countByCategoryId(Long categoryId);

    @Query("select coalesce(sum(e.amount), 0) from Expense e " +
            "where e.user.id = :userId and e.kind = :kind and e.expenseDate between :from and :to")
    BigDecimal sumAmountBetween(@Param("userId") Long userId,
                                 @Param("kind") Expense.TransactionKind kind,
                                 @Param("from") LocalDate from,
                                 @Param("to") LocalDate to);

    @Query("select e.category.id, e.category.name, e.category.color, coalesce(sum(e.amount), 0) " +
            "from Expense e where e.user.id = :userId and e.kind = :kind " +
            "and e.expenseDate between :from and :to " +
            "group by e.category.id, e.category.name, e.category.color order by sum(e.amount) desc")
    List<Object[]> sumByCategoryBetween(@Param("userId") Long userId,
                                         @Param("kind") Expense.TransactionKind kind,
                                         @Param("from") LocalDate from,
                                         @Param("to") LocalDate to);

    List<Expense> findByUserIdAndExpenseDateBetweenOrderByExpenseDateDesc(Long userId, LocalDate from, LocalDate to);

    @Query("select coalesce(sum(e.amount), 0) from Expense e where e.kind = :kind")
    BigDecimal sumAllAmountByKind(@Param("kind") Expense.TransactionKind kind);
}
