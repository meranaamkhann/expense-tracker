package com.asad.expensetracker.service;

import com.asad.expensetracker.exception.ResourceNotFoundException;
//import com.asad.expensetracker.exception.ResourceNotFoundException;
import com.asad.expensetracker.model.Expense;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExpenseService {

    private List<Expense> expenses = new ArrayList<>();
    private long nextId = 1;

    public Expense addExpense(Expense expense) {
        expense.setId(nextId++);
        expenses.add(expense);
        return expense;
    }

    public List<Expense> getAllExpenses() {
        return expenses;
    }

    public Expense updateExpense(long id, Expense updatedExpense) {
    for (Expense expense : expenses) {
        if (expense.getId() == id) {
            expense.setTitle(updatedExpense.getTitle());
            expense.setAmount(updatedExpense.getAmount());
            expense.setCategory(updatedExpense.getCategory());
            return expense;
            }
        }
    throw new ResourceNotFoundException("Expense not found with id " + id);
    }

public void deleteExpense(long id) {
    boolean removed = expenses.removeIf(expense -> expense.getId() == id);
    if (!removed) {
        throw new ResourceNotFoundException("Expense not found with id " + id);
        }
    }   
}
