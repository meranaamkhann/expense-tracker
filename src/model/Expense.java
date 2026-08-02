package model;
// Model class representing a single expense entry 
// Holds basic expense data like title,amount and category
public class Expense {
   public String title;
    public double amount;
   public String category;

   public Expense(String title, double amount, String category) {
        this.title = title;
        this.amount = amount;
        this.category = category;
    }
}