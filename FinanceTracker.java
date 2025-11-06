import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Main class for the Simplified Command-Line Finance Tracker.
 * Contains the main method, all CLI menu logic, and nested static classes
 * for the data models and service managers.
 *
 * This single file can be compiled with `javac FinanceTracker.java`
 * and run with `java FinanceTracker`.
 */
public class FinanceTracker {

    private static Scanner scanner = new Scanner(System.in);
    private static PersistenceManager pm = new PersistenceManager();
    private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println(" Welcome to the CLI Finance Tracker (V1) ");
        System.out.println("=========================================");

        while (true) {
            System.out.println("\n--- Main Menu ---");
            System.out.println("1. Login");
            System.out.println("2. Register");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    handleLogin();
                    break;
                case "2":
                    handleRegister();
                    break;
                case "3":
                    System.out.println("Thank you for using Finance Tracker. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please try again.");
            }
        }
    }

    /**
     * Handles the user login process.
     */
    private static void handleLogin() {
        System.out.print("Enter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine(); // In a real app, use Console.readPassword()

        User user = pm.loadUser(username);
        if (user != null && user.checkPassword(password, pm)) {
            System.out.println("\nWelcome back, " + user.username + "!");
            showAppMenu(user);
        } else {
            System.out.println("Error: Invalid username or password.");
        }
    }

    /**
     * Handles the new user registration process.
     */
    private static void handleRegister() {
        System.out.print("Enter new username: ");
        String username = scanner.nextLine();
        if (pm.userExists(username)) {
            System.out.println("Error: This username is already taken.");
            return;
        }
        System.out.print("Enter new password: ");
        String password = scanner.nextLine();
        
        String passwordHash = pm.hashPassword(password);
        User newUser = new User(username, passwordHash);
        pm.saveUser(newUser);

        System.out.println("Registration successful! Please login.");
    }

    /**
     * Displays the main application menu after successful login.
     * @param user The currently logged-in user.
     */
    private static void showAppMenu(User user) {
        boolean loggedIn = true;
        while (loggedIn) {
            System.out.println("\n--- App Menu (" + user.username + ") ---");
            System.out.println("1. Add Transaction");
            System.out.println("2. View Transactions");
            System.out.println("3. Manage Accounts");
            System.out.println("4. Manage Categories");
            System.out.println("5. Manage Budgets");
            System.out.println("6. Run Reports");
            System.out.println("7. Logout");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine();

            try {
                switch (choice) {
                    case "1":
                        handleAddTransaction(user);
                        break;
                    case "2":
                        handleViewTransactions(user);
                        break;
                    case "3":
                        handleManageAccounts(user);
                        break;
                    case "4":
                        handleManageCategories(user);
                        break;
                    case "5":
                        handleManageBudgets(user);
                        break;
                    case "6":
                        handleRunReports(user);
                        break;
                    case "7":
                        loggedIn = false;
                        System.out.println("Logging out...");
                        break;
                    default:
                        System.out.println("Invalid choice. Please try again.");
                }
                // Save user data after every action
                if (loggedIn) {
                    pm.saveUser(user);
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // --- Menu Handler Methods ---

    private static void handleAddTransaction(User user) {
        if (user.accounts.isEmpty()) {
            System.out.println("Error: You must add an account first.");
            return;
        }
        if (user.categories.isEmpty()) {
            System.out.println("Error: You must add a category first.");
            return;
        }

        try {
            System.out.print("Enter amount (positive for income, negative for expense): ");
            double amount = Double.parseDouble(scanner.nextLine());

            System.out.print("Enter description: ");
            String description = scanner.nextLine();

            System.out.print("Enter date (yyyy-MM-dd, press Enter for today): ");
            String dateStr = scanner.nextLine();
            Date date = dateStr.isEmpty() ? new Date() : dateFormatter.parse(dateStr);

            System.out.println("Select Account:");
            Account selectedAccount = selectFromList(user.accounts);

            System.out.println("Select Category:");
            Category selectedCategory = selectFromList(user.categories);

            Transaction tx = new Transaction(amount, description, date, selectedCategory, selectedAccount);
            user.addTransaction(tx);
            System.out.println("Transaction added successfully.");

        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid amount.");
        } catch (ParseException e) {
            System.out.println("Error: Invalid date format. Please use yyyy-MM-dd.");
        }
    }

    private static void handleViewTransactions(User user) {
        System.out.println("\n--- Your Transactions ---");
        if (user.transactions.isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }
        for (Transaction tx : user.transactions) {
            System.out.println(tx);
        }
    }

    private static void handleManageAccounts(User user) {
        System.out.println("\n--- Manage Accounts ---");
        System.out.println("1. Add New Account");
        System.out.println("2. View All Accounts");
        System.out.print("Choose an option: ");
        String choice = scanner.nextLine();

        if (choice.equals("1")) {
            System.out.print("Enter account name (e.g., Checking, Credit Card): ");
            String name = scanner.nextLine();
            System.out.print("Enter initial balance: ");
            double balance = Double.parseDouble(scanner.nextLine());
            System.out.print("Is this an asset (Y/N)? (e.g., Checking=Y, Credit Card=N): ");
            boolean isAsset = scanner.nextLine().equalsIgnoreCase("Y");
            
            user.accounts.add(new Account(name, balance, isAsset));
            System.out.println("Account '" + name + "' added.");
        } else if (choice.equals("2")) {
            System.out.println("\n--- Your Accounts ---");
            if (user.accounts.isEmpty()) {
                System.out.println("No accounts found.");
                return;
            }
            for (Account acc : user.accounts) {
                System.out.println(acc);
            }
        }
    }

    private static void handleManageCategories(User user) {
        System.out.println("\n--- Manage Categories ---");
        System.out.println("1. Add New Category");
        System.out.println("2. View All Categories");
        System.out.print("Choose an option: ");
        String choice = scanner.nextLine();

        if (choice.equals("1")) {
            System.out.print("Enter category name (e.g., Groceries, Rent): ");
            String name = scanner.nextLine();
            user.categories.add(new Category(name));
            System.out.println("Category '" + name + "' added.");
        } else if (choice.equals("2")) {
            System.out.println("\n--- Your Categories ---");
             if (user.categories.isEmpty()) {
                System.out.println("No categories found.");
                return;
            }
            for (Category cat : user.categories) {
                System.out.println("- " + cat.name);
            }
        }
    }

    private static void handleManageBudgets(User user) {
        System.out.println("\n--- Manage Budgets ---");
        System.out.println("1. Set/Update Budget for a Category");
        System.out.println("2. View All Budgets");
        System.out.print("Choose an option: ");
        String choice = scanner.nextLine();

        if (choice.equals("1")) {
             if (user.categories.isEmpty()) {
                System.out.println("Error: You must add a category first.");
                return;
            }
            System.out.println("Select category to budget:");
            Category selectedCategory = selectFromList(user.categories);
            
            System.out.print("Enter monthly budget limit for " + selectedCategory.name + ": ");
            double limit = Double.parseDouble(scanner.nextLine());
            
            user.setBudget(selectedCategory, limit);
            System.out.println("Budget set successfully.");

        } else if (choice.equals("2")) {
            System.out.println("\n--- Your Budgets ---");
             if (user.budgets.isEmpty()) {
                System.out.println("No budgets set.");
                return;
            }
            // Update spent amounts before viewing
            user.updateAllBudgetSpentAmounts();
            for (Budget b : user.budgets) {
                System.out.println(b);
            }
        }
    }

    private static void handleRunReports(User user) {
        System.out.println("\n--- Run Reports ---");
        System.out.println("1. Net Worth Report");
        System.out.println("2. Monthly Spending by Category");
        System.out.print("Choose an option: ");
        String choice = scanner.nextLine();
        
        ReportGenerator rg = new ReportGenerator();

        if (choice.equals("1")) {
            String report = rg.generateNetWorthReport(user);
            System.out.println(report);
        } else if (choice.equals("2")) {
            String report = rg.generateSpendingReport(user);
            System.out.println(report);
        }
    }

    /**
     * Helper function to let a user select an item from a list.
     * @param <T> The type of item in the list
     * @param list The list to select from
     * @return The selected item
     */
    private static <T> T selectFromList(List<T> list) {
        if (list.isEmpty()) {
            throw new RuntimeException("Cannot select from an empty list.");
        }
        for (int i = 0; i < list.size(); i++) {
            System.out.println((i + 1) + ". " + list.get(i).toString());
        }
        System.out.print("Select by number: ");
        int choiceIndex = Integer.parseInt(scanner.nextLine()) - 1;
        
        if (choiceIndex < 0 || choiceIndex >= list.size()) {
            throw new RuntimeException("Invalid selection index.");
        }
        return list.get(choiceIndex);
    }

    // ====================================================================
    //
    // NESTED STATIC MODEL CLASSES
    // (All implement Serializable for persistence)
    //
    // ====================================================================

    /**
     * Represents the root user object.
     * This is the object that gets serialized to a file.
     */
    static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        String username;
        String passwordHash;
        List<Account> accounts;
        List<Transaction> transactions;
        List<Category> categories;
        List<Budget> budgets;

        public User(String username, String passwordHash) {
            this.username = username;
            this.passwordHash = passwordHash;
            this.accounts = new ArrayList<>();
            this.transactions = new ArrayList<>();
            this.categories = new ArrayList<>();
            this.budgets = new ArrayList<>();
        }

        public boolean checkPassword(String password, PersistenceManager pm) {
            return this.passwordHash.equals(pm.hashPassword(password));
        }

        public void addTransaction(Transaction tx) {
            this.transactions.add(tx);
            // Update the balance of the associated account
            tx.account.balance += tx.amount;
        }

        public void setBudget(Category category, double limit) {
            // Remove old budget if it exists
            budgets.removeIf(b -> b.category.equals(category));
            // Add new budget
            budgets.add(new Budget(category, limit));
        }
        
        /**
         * Recalculates the 'spentAmount' for all budgets based on transactions.
         */
        public void updateAllBudgetSpentAmounts() {
            // This is a simple implementation. A real one would filter by month.
            for (Budget budget : budgets) {
                double spent = 0;
                for (Transaction tx : transactions) {
                    // Check if transaction is in the same category and is an expense
                    if (tx.category.equals(budget.category) && tx.amount < 0) {
                        // We use Math.abs because amount is negative
                        spent += Math.abs(tx.amount);
                    }
                }
                budget.spentAmount = spent;
            }
        }
    }

    /**
     * Represents a financial account (e.g., Checking, Credit Card).
     */
    static class Account implements Serializable {
        private static final long serialVersionUID = 1L;
        String accountName;
        double balance;
        boolean isAsset; // True = Asset (Checking), False = Liability (Credit Card)

        public Account(String accountName, double balance, boolean isAsset) {
            this.accountName = accountName;
            this.balance = balance;
            this.isAsset = isAsset;
        }

        @Override
        public String toString() {
            return String.format("%s (%s): $%.2f", 
                accountName, isAsset ? "Asset" : "Liability", balance);
        }
    }

    /**
     * Represents a single transaction (income or expense).
     */
    static class Transaction implements Serializable {
        private static final long serialVersionUID = 1L;
        double amount;
        String description;
        Date date;
        Category category;
        Account account;

        public Transaction(double amount, String description, Date date, Category category, Account account) {
            this.amount = amount;
            this.description = description;
            this.date = date;
            this.category = category;
            this.account = account;
        }

        @Override
        public String toString() {
            String type = amount >= 0 ? "INCOME" : "EXPENSE";
            String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(date);
            return String.format("[%s] %s: $%.2f - %s (Cat: %s, Acct: %s)",
                dateStr, type, Math.abs(amount), description, category.name, account.accountName);
        }
    }

    /**
     * Represents a simple, flat spending category.
     */
    static class Category implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;

        public Category(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Category category = (Category) obj;
            return name.equals(category.name);
        }
    }

    /**
     * Represents a monthly budget for a specific category.
     */
    static class Budget implements Serializable {
        private static final long serialVersionUID = 1L;
        Category category;
        double limitAmount;
        double spentAmount; // This would be calculated

        public Budget(Category category, double limitAmount) {
            this.category = category;
            this.limitAmount = limitAmount;
            this.spentAmount = 0; // Calculated on demand
        }

        @Override
        public String toString() {
            double remaining = limitAmount - spentAmount;
            return String.format("Budget for '%s': $%.2f spent of $%.2f ($%.2f remaining)",
                category.name, spentAmount, limitAmount, remaining);
        }
    }

    // ====================================================================
    //
    // NESTED STATIC SERVICE CLASSES
    //
    // ====================================================================

    /**
     * Handles saving and loading user data via serialization.
     * Also handles password security.
     */
    static class PersistenceManager {

        private static final String SAVE_DIR = "."; // Save in current directory
        private static final String FILE_EXT = ".ser";

        private String getFilePath(String username) {
            return SAVE_DIR + File.separator + username + FILE_EXT;
        }
        
        public boolean userExists(String username) {
            return new File(getFilePath(username)).exists();
        }

        /**
         * Saves the user object to a file.
         */
        public void saveUser(User user) {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getFilePath(user.username)))) {
                oos.writeObject(user);
            } catch (IOException e) {
                System.out.println("Error saving user data: " + e.getMessage());
            }
        }

        /**
         * Loads a user object from a file.
         */
        public User loadUser(String username) {
            if (!userExists(username)) {
                return null;
            }
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(getFilePath(username)))) {
                return (User) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Error loading user data: " + e.getMessage());
                return null;
            }
        }

        /**
         * Hashes a password using SHA-256 for secure storage.
         */
        public String hashPassword(String password) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
                // Convert byte array to hex string
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xff & b);
                    if(hex.length() == 1) hexString.append('0');
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("SHA-256 algorithm not found", e);
            }
        }
    }

    /**
     * Generates formatted text reports for the console.
     */
    static class ReportGenerator {

        public String generateNetWorthReport(User user) {
            double totalAssets = 0;
            double totalLiabilities = 0;

            for (Account acc : user.accounts) {
                if (acc.isAsset) {
                    totalAssets += acc.balance;
                } else {
                    // Liabilities are stored as positive balances, but represent debt
                    totalLiabilities += acc.balance; 
                }
            }
            double netWorth = totalAssets - totalLiabilities;

            StringBuilder sb = new StringBuilder();
            sb.append("\n--- Net Worth Report ---\n");
            sb.append(String.format("Total Assets:      $%.2f\n", totalAssets));
            sb.append(String.format("Total Liabilities: $%.2f\n", totalLiabilities));
            sb.append("------------------------\n");
            sb.append(String.format("Net Worth:         $%.2f\n", netWorth));
            return sb.toString();
        }

        public String generateSpendingReport(User user) {
            // Update spent amounts before generating the report
            user.updateAllBudgetSpentAmounts();
            
            StringBuilder sb = new StringBuilder();
            sb.append("\n--- Monthly Spending by Category Report ---\n");
            
            if (user.budgets.isEmpty()) {
                sb.append("No budgets set. Please set budgets to see this report.\n");
                return sb.toString();
            }

            for (Budget b : user.budgets) {
                sb.append(b.toString()).append("\n");
            }
            return sb.toString();
        }
    }
}