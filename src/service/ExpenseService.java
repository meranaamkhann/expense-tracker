package service;

import model.Expense;
import repository.ExpenseRepository;
import java.util.ArrayList;
import java.util.HashMap;

// Service layer containing bussiness logic for expense management
// Acts as a bridge between Main and Repository
public class ExpenseService {

    private ArrayList<Expense> expenses;
    private ExpenseRepository repo = new ExpenseRepository();

    public ExpenseService() {
        expenses = repo.loadExpenses();
    }

    // Adds a new expense
    public void addExpense(Expense e) {
        expenses.add(e);
    }

    public ArrayList<Expense> getAllExpenses() {
        return expenses;
    }

    // Returns total amount of all expenses
    public double getTotal() {
        double total = 0;
        for (Expense e : expenses) {
            total += e.amount;
        }
        return total;
    }

    // Updates category-wise expense summary
    public HashMap<String, Double> categorySummary() {
        HashMap<String, Double> map = new HashMap<>();
        for (Expense e : expenses) {
            map.put(e.category, map.getOrDefault(e.category, 0.0) + e.amount);
        }
        return map;
    }

    // Delete an existing expense
    public void deleteExpense(int index) {
        if (index >= 0 && index < expenses.size()) {
            expenses.remove(index);
        }
    }

    //Persists data before application exists
    public void save() {
        repo.saveExpenses(expenses);
    }

    //Updates the amount of an existing expense
    public void updateAmount(int index, double newAmount) {
    if (index >= 0 && index < expenses.size()) {
        expenses.get(index).amount = newAmount;
    } else {
        System.out.println("Invalid expense number.");
    }
    }
}