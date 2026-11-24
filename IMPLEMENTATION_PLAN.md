# Mini Lending Credit Line — Backend Plan

## Goal and scope

Implement the backend flow in `mini-lending-credit-line-30h.md`: demo user submits financial information, a deterministic policy approves or rejects a credit line, approved users utilize that limit through drawdowns, and four monthly principal instalments restore limit as they are paid. All amounts are simulated SGD; there is no cash transfer or real credit decision.

This branch replaces the earlier product checkout/credit reservation model. The former PayLater implementation remains on `develop`. Backend work is isolated on `credit-line-rebuild`; frontend is intentionally out of scope for this branch.

## Code conventions and architecture

- Java 21, Spring Boot 3.5, PostgreSQL 17, Spring Data JPA, Flyway, Maven.
- Feature packages: `application`, `assessment`, `credit`, `drawdown`, `instalment`, `repayment`; shared `common` and `config`.
- Controllers return typed DTOs inside `ApiResponse<T>`; business transactions live in services; entities never form the HTTP contract.
- Bean Validation handles request shape. `AppException` and `ErrorCode` map domain failures in `GlobalExceptionHandler`.
- Constructor injection via Lombok `@RequiredArgsConstructor` and `@FieldDefaults`, as in the previous backend and the `e-learning`/`BKJob` references.
- One seeded demo user. No authentication, scoring model, interest, fee, partial repayment, early full settlement or real payment integration.

## Business rules

### Assessment policy `DEMO_V1`

`surplus = monthlyNetIncome - monthlyLivingExpenses - monthlyDebtPayments`.

| Surplus (SGD) | Decision | Limit | Reason |
|---:|---|---:|---|
| `< 400` | REJECTED | 0 | `INSUFFICIENT_SURPLUS` |
| `400` to `< 800` | APPROVED | 500 | `APPROVED_TIER_500` |
| `800` to `< 1500` | APPROVED | 1000 | `APPROVED_TIER_1000` |
| `>= 1500` | APPROVED | 2000 | `APPROVED_TIER_2000` |

Persist input amounts, computed surplus, reason, policy version and assessed time. An approved application creates exactly one `ACTIVE` account in the same transaction; the user row is locked to serialize concurrent submissions. A rejection can be followed by another assessment. After approval, further submissions return 409.

### Credit line and drawdown

`availableLimit = totalLimit - usedLimit`; `usedLimit` is outstanding principal. An account row lock serializes drawdown and repayment changes. Drawdown requires `0.04 <= amount <= availableLimit`, two decimal places, and an active account. SGD 0.04 is the smallest amount divisible into four **positive** cent-denominated instalments. The drawdown, account update, schedule and idempotency result commit together.

### Schedule and repayment

Four instalments are due at drawdown date plus 1, 2, 3 and 4 calendar months. The first three equal `amount / 4` rounded down to cents; the fourth receives the remainder. Each repayment must target the earliest unpaid instalment, pays its full amount, reduces both drawdown outstanding principal and account used limit, and inserts one immutable `SUCCESS` transaction. The last repayment marks the drawdown `PAID_OFF`.

### Idempotency and concurrency

`CREATE_DRAWDOWN` and `REPAY_INSTALMENT` use `Idempotency-Key`. A PostgreSQL transaction-scoped advisory lock protects first use of an operation/key pair. The database enforces `UNIQUE(operation, idempotency_key)` and one repayment per instalment. Same key + same payload replays the original JSON response; same key + different payload returns 409. Balance-changing transactions lock in account → drawdown → instalment order. Account `CHECK` constraints prevent negative or over-limit used balance if application logic regresses.

## Database and Flyway

- `V1__create_credit_line_schema.sql`: six core tables (`users`, `credit_applications`, `credit_accounts`, `credit_drawdowns`, `instalments`, `repayment_transactions`) plus `idempotency_records`; foreign keys, unique keys, money/state checks and query indexes.
- `V2__seed_demo_user.sql`: stable demo user UUID `00000000-0000-0000-0000-000000000001`. No account is pre-created; application approval creates it.
- Hibernate `ddl-auto: validate` checks mappings at startup but does not create or alter tables.
- Compose uses a separate `lending_credit_line` database, port `55433` and volume from the PayLater branch. This prevents an older Flyway history from rejecting the new `V1` checksum and preserves the earlier demo data.
- After a migration is applied to a shared or persisted database, add a new `V3`, `V4`, etc. rather than editing an applied migration.

## REST contract

| Method | Endpoint | Result |
|---|---|---|
| POST | `/api/credit-applications` | Assess and optionally create account (201) |
| GET | `/api/credit-applications/latest` | Latest decision |
| GET | `/api/credit-account` | Total, used and available limit |
| POST | `/api/credit-drawdowns` | Utilize amount and create 4 instalments (201; key required) |
| GET | `/api/credit-drawdowns` | List drawdowns |
| GET | `/api/credit-drawdowns/{id}` | Drawdown and instalment schedule |
| POST | `/api/instalments/{id}/repay` | Pay next full instalment (key required) |
| GET | `/api/credit-drawdowns/{id}/repayments` | Repayment history |

All successful responses use `ApiResponse<T>`. Validation errors return 400, missing resources 404, and conflicting states/limit/idempotency return 409.

## Verification and delivery

1. Unit test the four assessment boundaries and schedule rounding/dates.
2. PostgreSQL/Testcontainers integration test approval, account creation, drawdown 800, limit 1000, full four-payment lifecycle, idempotent retries and out-of-order repayment.
3. Race two SGD 700 drawdowns on a SGD 1000 line; exactly one succeeds.
4. MockMvc test HTTP envelope, status codes and end-to-end flow.
5. `mvn clean verify`; start Compose and run the README demo against the local database.
6. Commit and push completed backend work to `credit-line-rebuild`, keeping `develop` unchanged.
