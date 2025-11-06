# personal-finance-tracker — Detailed Project Overview

This README provides a detailed walk-through of the Java CLI project in this folder (`FinanceTracker.java`).
It explains how the main model classes interact (User, Account, Transaction, Category, Budget),
shows commented versions of each key class, counts total classes and lines of code, and maps
where core OOP concepts are used.

---

## Quick facts (auto-detected)

- Project file: `FinanceTracker.java`
- Total lines (source): 607
- Total classes (compiled): 8

---

## PART 1 — How Account, Transaction, and User interact

High level relationship (composition):

User
 ├── List<Account> accounts
 ├── List<Transaction> transactions
 ├── List<Category> categories
 └── List<Budget> budgets

When you add a transaction the code does:

public void addTransaction(Transaction tx) {
	this.transactions.add(tx);
	tx.account.balance += tx.amount;
}

Step-by-step explanation:
- A `Transaction` object is constructed (amount, category, account, date, description).
- `user.addTransaction(tx)` is called.
  - The `Transaction` object is appended to the user's `transactions` list.
  - The `Account` referenced by `tx.account` has its `balance` field updated by `tx.amount`.
	- If `tx.amount` is negative (expense), the balance decreases.
	- If `tx.amount` is positive (income), the balance increases.

This is classic object composition and direct object interaction: the `User` contains `Account` and
`Transaction` objects; a `Transaction` refers back to its `Account` and `Category`.

Why this matters:
- Responsibility is clear: `User` owns the lists and orchestrates updates.
- `Transaction` does not update persistence or budgets itself; the `User` coordinates that.

---

## PART 2 — Commented key classes (line-by-line notes for important lines)

Below are the important model and service classes rewritten with explanatory comments for
the key fields and methods. This focuses on the important lines you asked about (constructors,
mutators, toString, persistence, and budget updates). It is not a literal source transformation
— it's an explanatory version you can read through to understand the code.

### Account (manages balance and account info)

```java
// Represents a financial account such as Checking, Savings, or Credit Card
static class Account implements Serializable {
	private static final long serialVersionUID = 1L; // serialization version control

	// Fields
	String accountName; // user-visible account name
	double balance;     // numeric balance; positive numbers represent amount held/owed depending on isAsset
	boolean isAsset;    // true => asset (e.g., bank account), false => liability (e.g., credit card)

	// Constructor: initialize the fields when creating a new Account
	public Account(String accountName, double balance, boolean isAsset) {
		this.accountName = accountName; // assign constructor param to instance field
		this.balance = balance;         // set initial balance
		this.isAsset = isAsset;         // mark asset vs liability
	}

	// toString: returns a short readable description used when printing lists
	@Override
	public String toString() {
		// shows name, type (Asset/Liability), and formatted balance with 2 decimals
		return String.format("%s (%s): $%.2f", 
			accountName, isAsset ? "Asset" : "Liability", balance);
	}
}
```

Notes:
- `balance` is directly mutated by code elsewhere (e.g., `tx.account.balance += tx.amount`).
- In a stricter encapsulation model you'd make `balance` private and expose deposit/withdraw methods.

### Transaction (records income or expense)

```java
static class Transaction implements Serializable {
	private static final long serialVersionUID = 1L;

	double amount;       // Positive => income, Negative => expense
	String description;  // Free-text description
	Date date;           // When it occurred
	Category category;   // The spending category
	Account account;     // The account affected by this transaction

	public Transaction(double amount, String description, Date date, Category category, Account account) {
		this.amount = amount;               // store signed amount
		this.description = description;     // store description
		this.date = date;                   // store date object
		this.category = category;           // reference the category object
		this.account = account;             // reference the account object
	}

	@Override
	public String toString() {
		// Show whether it's income or expense, the absolute amount, description,
		// category name, and account name for readable CLI output.
		String type = amount >= 0 ? "INCOME" : "EXPENSE";
		String dateStr = new SimpleDateFormat("yyyy-MM-dd").format(date);
		return String.format("[%s] %s: $%.2f - %s (Cat: %s, Acct: %s)",
			dateStr, type, Math.abs(amount), description, category.name, account.accountName);
	}
}
```

Notes:
- `Transaction` does not mutate any persistence or budget state itself. It is a data holder.
- The `User`'s `addTransaction` method is responsible for updating lists and account balance.

### User (stores user data and performs operations)

```java
static class User implements Serializable {
	private static final long serialVersionUID = 1L;

	String username;                // login name
	String passwordHash;            // hashed password (SHA-256 in PersistenceManager)
	List<Account> accounts;         // all accounts owned by user
	List<Transaction> transactions; // chronological transaction list
	List<Category> categories;      // spending categories
	List<Budget> budgets;           // category budgets

	public User(String username, String passwordHash) {
		this.username = username;             // user id
		this.passwordHash = passwordHash;     // store hashed password
		this.accounts = new ArrayList<>();    // empty containers on creation
		this.transactions = new ArrayList<>();
		this.categories = new ArrayList<>();
		this.budgets = new ArrayList<>();
	}

	public boolean checkPassword(String password, PersistenceManager pm) {
		// Ask the persistence manager to hash the candidate and compare with stored hash
		return this.passwordHash.equals(pm.hashPassword(password));
	}

	public void addTransaction(Transaction tx) {
		// Add transaction to user's list (local memory)
		this.transactions.add(tx);
		// Mutate the linked account's balance directly
		tx.account.balance += tx.amount;
	}

	public void setBudget(Category category, double limit) {
		// Remove any existing budget for that category and add the new one
		budgets.removeIf(b -> b.category.equals(category));
		budgets.add(new Budget(category, limit));
	}

	public void updateAllBudgetSpentAmounts() {
		// For each budget, sum the absolute values of negative (expense) transactions
		for (Budget budget : budgets) {
			double spent = 0;
			for (Transaction tx : transactions) {
				if (tx.category.equals(budget.category) && tx.amount < 0) {
					spent += Math.abs(tx.amount);
				}
			}
			budget.spentAmount = spent;
		}
	}
}
```

Notes:
- `User` is the coordinator: it keeps collections and performs operations that update the
  objects contained (composition).
- There is an opportunity to better encapsulate balance updates with `Account` methods.

### Category (simple identity object)

```java
static class Category implements Serializable {
	private static final long serialVersionUID = 1L;
	String name; // category name, e.g. "Groceries"

	public Category(String name) { this.name = name; }

	@Override
	public boolean equals(Object obj) {
		// Equality is based on the category name string
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		Category category = (Category) obj;
		return name.equals(category.name);
	}
}
```

Notes:
- Equality lets categories be compared/removed based on name.

### Budget (tracks limit and spent)

```java
static class Budget implements Serializable {
	private static final long serialVersionUID = 1L;
	Category category;   // linked category object
	double limitAmount;  // monthly limit
	double spentAmount;  // calculated on demand

	public Budget(Category category, double limitAmount) {
		this.category = category;
		this.limitAmount = limitAmount;
		this.spentAmount = 0;
	}

	@Override
	public String toString() {
		double remaining = limitAmount - spentAmount;
		return String.format("Budget for '%s': $%.2f spent of $%.2f ($%.2f remaining)",
			category.name, spentAmount, limitAmount, remaining);
	}
}
```

### PersistenceManager (save/load and hashing)

```java
static class PersistenceManager {
	private static final String SAVE_DIR = "."; // current directory
	private static final String FILE_EXT = ".ser";

	private String getFilePath(String username) {
		return SAVE_DIR + File.separator + username + FILE_EXT;
	}

	public boolean userExists(String username) {
		return new File(getFilePath(username)).exists();
	}

	public void saveUser(User user) {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(getFilePath(user.username)))) {
			oos.writeObject(user); // serialize the entire User object graph
		} catch (IOException e) {
			System.out.println("Error saving user data: " + e.getMessage());
		}
	}

	public User loadUser(String username) {
		if (!userExists(username)) return null;
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(getFilePath(username)))) {
			return (User) ois.readObject(); // deserialize back into a User
		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Error loading user data: " + e.getMessage());
			return null;
		}
	}

	public String hashPassword(String password) {
		// SHA-256 hashing to avoid storing plain-text passwords
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
		// convert to hex string
		StringBuilder hexString = new StringBuilder();
		for (byte b : hash) {
			String hex = Integer.toHexString(0xff & b);
			if(hex.length() == 1) hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}
}
```

Notes:
- Uses Java serialization (ObjectOutputStream/ObjectInputStream) — simple but not portable to other languages.
- Passwords are hashed with SHA-256; salting would be a recommended improvement for real apps.

### ReportGenerator (console reports)

```java
static class ReportGenerator {
	public String generateNetWorthReport(User user) {
		double totalAssets = 0;
		double totalLiabilities = 0;
		for (Account acc : user.accounts) {
			if (acc.isAsset) totalAssets += acc.balance;
			else totalLiabilities += acc.balance;
		}
		double netWorth = totalAssets - totalLiabilities;
		// Build a formatted text report
		StringBuilder sb = new StringBuilder();
		sb.append("\n--- Net Worth Report ---\n");
		sb.append(String.format("Total Assets:      $%.2f\n", totalAssets));
		sb.append(String.format("Total Liabilities: $%.2f\n", totalLiabilities));
		sb.append("------------------------\n");
		sb.append(String.format("Net Worth:         $%.2f\n", netWorth));
		return sb.toString();
	}
}
```

Notes:
- `generateSpendingReport` uses `user.updateAllBudgetSpentAmounts()` first to ensure numbers are current.

---

## PART 3 — Counts and LOC

- Total classes (outer + nested compiled): 8
  - `FinanceTracker` (outer)
  - Nested: `User`, `Account`, `Transaction`, `Category`, `Budget`, `PersistenceManager`, `ReportGenerator`
- Total lines in `FinanceTracker.java`: 607

These numbers were measured from the repository source and compiled class files in the project folder.

---

## PART 4 — OOP concepts used and where

Encapsulation
- Each conceptual entity (User, Account, Transaction, Budget, Category) groups data and behavior.
- Note: many fields are package-access (no `private`) and mutated directly (e.g., `tx.account.balance += tx.amount`).
  Improving encapsulation: make fields `private` and add methods like `deposit`/`withdraw` on `Account`.

Abstraction
- `PersistenceManager` abstracts file save/load and hashing behind simple methods (`saveUser`, `loadUser`, `hashPassword`).
- `ReportGenerator` abstracts report formatting.

Inheritance
- Classes implement `Serializable` (interface inheritance) to allow serialization.
- There is no class-to-class inheritance hierarchy (no `extends` usage) beyond the default `Object` parent.

Polymorphism
- `toString()` overrides across several classes (Account, Transaction, Budget, Category): printing a reference calls the object's own implementation.
- This is runtime polymorphism: the invoked method depends on the object's actual runtime class.

Composition (has-a)
- `User` has `List<Account>` and `List<Transaction>`, `Transaction` has an `Account` and `Category`. This is composition — objects contain other objects.

Design notes & suggested improvements
- Encapsulate balance changes: add `deposit(double)` and `withdraw(double)` to `Account` to centralize validation/formatting.
- Improve persistence: use JSON (for readability) or a database for multi-user support and portability.
- Add password salting and iterations (PBKDF2/Bcrypt/Argon2) rather than bare SHA-256.

---

## PART 5 — Where to go next

- If you'd like, I can:
  - Commit this README update (I will commit locally and can push if you confirm),
  - Refactor `Account` to add encapsulated deposit/withdraw methods and update call-sites,
  - Replace Java serialization with JSON persistence, or
  - Add unit tests for `User.addTransaction`, `Budget` updates, and `ReportGenerator`.

Tell me which follow-up you'd like and I'll proceed.

---

End of README

# personal-finance-tracker
