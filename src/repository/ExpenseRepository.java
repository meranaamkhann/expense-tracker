package repository;

import model.Expense;
import java.io.*;
import java.util.ArrayList;

// repository class responsible for data persistence
// Handles saving and loading expensses from a file
public class ExpenseRepository {

    private final String FILE_NAME = "expenses.txt";
    // Loads all expenses from the file into memory
    public ArrayList<Expense> loadExpenses() {
        ArrayList<Expense> list = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                String[] parts = line.split(",");
                if (parts.length != 3) continue;

                list.add(new Expense(
                        parts[0],
                        Double.parseDouble(parts[1]),
                        parts[2]
                ));
            }
        } catch (IOException e) {
            // ignore
        }

        return list;
    }
    // Saves the current list of expenses to the file
    public void saveExpenses(ArrayList<Expense> expenses) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Expense e : expenses) {
                bw.write(e.title + "," + e.amount + "," + e.category);
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error saving data");
        }
    }
}