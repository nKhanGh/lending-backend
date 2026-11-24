# Credit Line MVP backend plan

Scope source: the user-provided `mini-lending-credit-line-30h.md`. This branch starts from backend bootstrap `4d79842` and covers only the backend portion of that MVP.

1. Create the six tables from section 5.2 with PostgreSQL Flyway constraints and seed one demo user.
2. Add JPA entities and repositories for applications, accounts, drawdowns, instalments, and repayments.
3. Implement `DEMO_V1` assessment boundaries; persist every decision and create one account on approval.
4. Expose account total, used, and available limit; serialize balance mutations with an account row lock.
5. Implement idempotent drawdown in one transaction with four monthly principal instalments and exact cent allocation.
6. Implement full payment of the earliest unpaid instalment; persist a success record and restore the limit in the same transaction.
7. Expose the eight REST endpoints listed in section 6 using the existing response and exception conventions.
8. Verify policy thresholds, cent rounding, insufficient limit, concurrent requests, retries, repayment ordering, and balance invariants.
9. Document setup, API contract, and the `3000 / 1400 / 500` demo scenario.

Frontend, authentication, multiple users, partial or asynchronous repayment, fees, overdue processing, ledger, audit subsystem, and policy V2 are outside this branch's scope.
