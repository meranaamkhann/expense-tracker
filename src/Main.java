import model.Expense;
import service.ExpenseService;
import java.util.Scanner;
import java.util.HashMap;

//Entry pont of the application 
// Handles user interaction and delegates logic to the sservice layer

public class Main {
    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        ExpenseService service = new ExpenseService();

        while (true) {
            System.out.println("\n1. Add Expense");
            System.out.println("2. View Expenses");
            System.out.println("3. Category Summary");
            System.out.println("4. Delete Expense");
            System.out.println("5. Edit Expense");
            System.out.println("6. Exit");
            int choice = sc.nextInt();
            sc.nextLine();

            if (choice == 1) {
                System.out.print("Title: ");
                String title = sc.nextLine();

                System.out.print("Amount: ");
                double amount = sc.nextDouble();
                sc.nextLine();
                if (amount <= 0) {
                System.out.println("Amount must be greater than 0.");
                continue;
                }

                System.out.print("Category: ");
                String category = sc.nextLine();

                service.addExpense(new Expense(title, amount, category));
            }

            else if (choice == 2) {
                int i = 1;
                for (Expense e : service.getAllExpenses()) {
                    System.out.println(i++ + ". " + e.title + " | ₹" + e.amount + " | " + e.category);
                }
                System.out.println("Total: ₹" + service.getTotal());
            }

            else if (choice == 3) {
                HashMap<String, Double> map = service.categorySummary();
                for (String k : map.keySet()) {
                    System.out.println(k + " : ₹" + map.get(k));
                }
            }

            else if (choice == 4) {
                System.out.print("Enter number to delete: ");
                int idx = sc.nextInt();
                service.deleteExpense(idx - 1);
            }
            else if (choice == 5) {
            System.out.print("Enter expense number to edit: ");
            int idx = sc.nextInt();
            sc.nextLine();

            System.out.print("Enter new amount: ");
            double newAmount = sc.nextDouble();
            sc.nextLine();

            if (newAmount <= 0) {
            System.out.println("Amount must be greater than 0.");
            continue;
        }

            service.updateAmount(idx - 1, newAmount);
        }

            else if (choice == 6) {
                service.save();
                System.out.println("Goodbye!");
                break;
            }
        }
    }
}

