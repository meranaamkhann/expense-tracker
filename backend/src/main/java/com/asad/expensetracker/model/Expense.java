package com.asad.expensetracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "expenses", indexes = {
        @Index(name = "idx_expense_user_date", columnList = "user_id, expense_date"),
        @Index(name = "idx_expense_user_category", columnList = "user_id, category_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 160)
    private String title;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 14, scale = 2)
    private java.math.BigDecimal amount;

    /** ISO 4217 currency code. Defaults to INR for this deployment. */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private TransactionKind kind = TransactionKind.EXPENSE;

    @Column(length = 500)
    private String notes;

    @NotNull
    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum TransactionKind {
        EXPENSE, INCOME
    }
}
