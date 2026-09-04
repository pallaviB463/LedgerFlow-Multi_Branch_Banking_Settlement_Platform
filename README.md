# LedgerFlow - Multi-Branch Banking & Settlement Platform

Welcome to the backend repository for **LedgerFlow**, a robust, core banking platform designed to simulate multi-branch banking, real-time transaction processing, and delayed ACH-style settlements.

This backend was built by the Core Banking Engineer to ensure strict adherence to financial principles, high concurrency, and data integrity. It is now ready for the Frontend/Integration Engineer to build the user interface and connect it to these APIs!

## 🚀 Tech Stack
*   **Language:** Java 17 (using modern features like Records)
*   **Framework:** Spring Boot 3.2.5
*   **Database:** H2 (In-memory, easily swapable to PostgreSQL) / Spring Data JPA
*   **Build Tool:** Maven

## 🏗️ What Was Built (Backend Highlights)

1.  **Double-Entry Ledger System:**
    *   Strict adherence to ACID principles and double-entry bookkeeping.
    *   Every transaction creates immutable `CREDIT` and `DEBIT` entries in the `ledger_entries` table.
    *   Balances are calculated dynamically from the ledger to prevent tampering.
2.  **Concurrency Control (Pessimistic Locking):**
    *   Implemented `PESSIMISTIC_WRITE` locks on accounts during transfers.
    *   Prevents race conditions and double-spending when multiple transfers hit the same account simultaneously.
3.  **ACH Settlement & Saga Pattern:**
    *   Implemented asynchronous batch processing for ACH transfers.
    *   **Saga Pattern:** 
        1. *Initiation:* Funds are reserved (debited from sender, credited to clearing).
        2. *Settlement:* Batch is processed, clearing is debited, receiver is credited.
        3. *Compensation:* If settlement fails, a compensating transaction automatically returns funds to the sender.
4.  **Reporting & Auditing:**
    *   Real-time Trial Balance generation (`ReportService`).
    *   Detailed Account Statements showing running balances after every transaction.

## 🛠️ How to Run the Backend

1.  Ensure you have **Java 17** installed.
2.  Clone the repository.
3.  Open a terminal in the project root directory.
4.  Run the application using the Maven wrapper:
    ```bash
    ./mvnw spring-boot:run
    ```
    *(On Windows, use `.\mvnw.cmd spring-boot:run`)*
5.  The server will start on `http://localhost:8080`.

## 🔌 API Endpoints for Frontend Integration

Here are the key REST API endpoints your frontend will need to interact with:

### Accounts & Branches
*   `POST /api/accounts` - Create a new bank account.
*   `GET /api/accounts/{accountNumber}` - Get account details and balance.

### Transfers (Real-Time RTGS)
*   `POST /api/transfers` - Initiate a real-time transfer between two accounts.
    *   *Body:* `{ "sourceAccountNumber": "...", "destinationAccountNumber": "...", "amount": 100.00 }`

### ACH (Delayed Settlement)
*   `POST /api/ach/transfers` - Submit an ACH transfer (reserves funds).
*   `POST /api/ach/batches/current/settle` - Process the current ACH batch (settles funds).
*   `GET /api/ach/batches/{id}/netting` - Get the netting report for a specific batch.

### Reports
*   `GET /api/reports/trial-balance` - Get the current trial balance of the entire bank.
*   `GET /api/reports/statement/{accountNumber}` - Get the detailed statement for an account.

## 👩‍💻 Next Steps 

Your focus will be on building the user interface and calling the APIs listed above. You can start by:
1.  Building a dashboard to view the Trial Balance (`/api/reports/trial-balance`).
2.  Building a form to create accounts (`/api/accounts`).
3.  Building a transfer interface for real-time and ACH transfers (`/api/transfers` and `/api/ach/transfers`).
4.  Building a view to see an individual account's statement (`/api/reports/statement/{accountNumber}`).

Good luck, and let the backend team know if you need any adjustments to the API payloads!
