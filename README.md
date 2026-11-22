# Mini PayLater Backend

A Grab-inspired PayLater simulator built with Java 21, Spring Boot, and PostgreSQL. The project focuses on the backend problems that make a lending checkout safe: atomic credit reservations, explicit state transitions, exact monetary arithmetic, idempotent retries, and concurrency-safe repayment.

The application is a learning proof of concept. It does not process real money and is not affiliated with Grab or NAB.

## What is implemented

- Seeded catalogue of 8 products; checkout prices always come from the database.
- One seeded demo user with an SGD 500.00 credit limit.
- Credit dashboard with total, outstanding, reserved, and available amounts.
- Checkout reservation with PostgreSQL row locking to prevent overspending.
- Idempotent checkout creation, confirmation, and repayment.
- Confirm, cancel, and automatic 10-minute reservation expiry.
- Four monthly instalments with exact remainder allocation.
- Full-instalment simulated repayment and automatic credit restoration.
- Early settlement of every remaining instalment on a confirmed order, with idempotent retries.
- Order, instalment, and repayment history APIs.
- Flyway-owned schema with monetary and uniqueness constraints.
- PostgreSQL/Testcontainers tests for retries, rollback, expiry, rounding, concurrent checkout, and concurrent payment.

## Financial model

The central invariant is:

```text
availableCredit = totalLimit - outstandingAmount - reservedAmount
outstandingAmount >= 0
reservedAmount >= 0
outstandingAmount + reservedAmount <= totalLimit
```

Every financial transition is committed in one database transaction. Money uses `BigDecimal` in Java and `NUMERIC(18,2)` in PostgreSQL.

Order state transitions:

```text
RESERVED ---- confirm ----> CONFIRMED
    |
    +-------- cancel -----> CANCELLED
    |
    +-------- expire -----> EXPIRED
```

For an SGD 100.01 purchase, the four instalments are `25.00`, `25.00`, `25.00`, and `25.01`, so rounding never loses money.

## Architecture

This is a modular monolith. Packages are grouped by business capability:

```text
com.nkhang.lending
├── catalog       product catalogue
├── credit        account balances and dashboard
├── checkout      reservation and order lifecycle
├── instalment    four-part schedule
├── repayment     simulated payments and history
├── common        API envelope, errors, idempotency
└── config        clock, OpenAPI, scheduling
```

The HTTP style follows the controller–service–repository conventions used in the sibling `e-learning` and `BKJob` projects. Entities never form the public API; controllers return purpose-built DTOs inside a shared `ApiResponse<T>` envelope.

### Concurrency strategy

- Credit-account updates use `SELECT ... FOR UPDATE`, serializing competing balance changes.
- Confirm, cancel, and expiry lock rows in the same order: order, reservation, account.
- Payment locks the instalment and then the account.
- Idempotency uses a PostgreSQL transaction-scoped advisory lock derived from `operation + Idempotency-Key`. This prevents two first-time requests with the same key from racing to insert the key.
- Database checks and unique constraints remain the final defence against invalid balances, duplicate schedules, and duplicate repayments.

## Requirements

- Java 21
- Maven 3.9+
- Docker Desktop or another Docker-compatible engine

## Run locally

1. Start PostgreSQL:

   ```bash
   docker compose up -d postgres
   ```

2. Start the backend:

   ```bash
   mvn spring-boot:run
   ```

3. Open Swagger UI at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).

The health endpoint is [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

Default local configuration:

| Variable | Default |
|---|---|
| `DB_HOST` | `localhost` |
| `DB_PORT` | `55432` |
| `DB_NAME` | `lending` |
| `DB_USERNAME` | `lending` |
| `DB_PASSWORD` | `lending` |
| `SERVER_PORT` | `8080` |

Copy `.env.example` when custom values are needed. Do not commit `.env`.

## Demo data

| Resource | ID | Amount |
|---|---|---:|
| Demo user | `00000000-0000-0000-0000-000000000001` | SGD 500.00 limit |
| Wireless Headphones | `00000000-0000-0000-0000-000000000201` | SGD 120.00 |
| Mechanical Keyboard | `00000000-0000-0000-0000-000000000202` | SGD 100.01 |

Eight products are inserted by `V2__seed_demo_data.sql`.

## Demo flow

The examples use `curl`. Each mutating retry-sensitive request needs a stable, client-generated `Idempotency-Key`.

1. Check the starting balance:

   ```bash
   curl http://localhost:8080/api/credit-account
   ```

2. Reserve SGD 120.00:

   ```bash
   curl -X POST http://localhost:8080/api/checkouts \
     -H "Content-Type: application/json" \
     -H "Idempotency-Key: demo-checkout-001" \
     -d '{"productId":"00000000-0000-0000-0000-000000000201","quantity":1}'
   ```

   Copy `result.checkoutId` from the response. Available credit is now SGD 380.00 and reserved credit is SGD 120.00.

3. Confirm and create four SGD 30.00 instalments:

   ```bash
   curl -X POST http://localhost:8080/api/checkouts/CHECKOUT_ID/confirm \
     -H "Idempotency-Key: demo-confirm-001"
   ```

   Replace `CHECKOUT_ID` with the checkout UUID. Copy an instalment ID from `result.instalments`.

4. Pay one instalment:

   ```bash
   curl -X POST http://localhost:8080/api/instalments/INSTALMENT_ID/pay \
     -H "Idempotency-Key: demo-repay-001"
   ```

   Outstanding becomes SGD 90.00 and available credit becomes SGD 410.00.

5. Inspect history:

   ```bash
   curl http://localhost:8080/api/orders
   curl http://localhost:8080/api/repayments
   ```

To settle all remaining instalments after step 4:

```bash
curl -X POST http://localhost:8080/api/orders/CHECKOUT_ID/settle \
  -H "Idempotency-Key: demo-settle-001"
```

This pays the remaining SGD 90.00, returns the three new repayment IDs, and restores available credit to SGD 500.00. Retrying with the same key returns the same settlement result.

To cancel instead of confirm:

```bash
curl -X POST http://localhost:8080/api/checkouts/CHECKOUT_ID/cancel
```

## API summary

| Method | Endpoint | Purpose |
|---|---|---|
| `GET` | `/api/products` | Active product catalogue |
| `GET` | `/api/credit-account` | Demo user's credit dashboard |
| `POST` | `/api/checkouts` | Reserve credit for a product |
| `POST` | `/api/checkouts/{id}/confirm` | Capture reservation and create schedule |
| `POST` | `/api/checkouts/{id}/cancel` | Release an unconfirmed reservation |
| `GET` | `/api/orders` | Order history |
| `GET` | `/api/orders/{id}` | Order, reservation, and instalment detail |
| `GET` | `/api/orders/{id}/instalments` | Ordered instalment schedule |
| `POST` | `/api/instalments/{id}/pay` | Pay one complete instalment |
| `POST` | `/api/orders/{id}/settle` | Pay every remaining instalment on a confirmed order |
| `GET` | `/api/repayments` | Repayment history |

Important domain errors include `INSUFFICIENT_CREDIT`, `INVALID_CHECKOUT_STATE`, `RESERVATION_EXPIRED`, `INSTALMENT_ALREADY_PAID`, `ORDER_ALREADY_SETTLED`, and `IDEMPOTENCY_KEY_REUSED`. The numeric `code` and readable `message` are returned in the standard response envelope.

## Test

Docker must be running because integration and concurrency tests use a real ephemeral PostgreSQL 17 instance:

```bash
mvn verify
```

The critical suites cover:

- schema migration and seed validation;
- catalogue and credit HTTP responses;
- checkout retry and idempotency-key conflict;
- insufficient-credit transaction rollback;
- two simultaneous checkouts competing for one limit;
- confirm retry, cancellation, expiry, and exact rounding;
- payment retry and two simultaneous payments for one instalment;
- early settlement after a prior payment, retry, and a race with single-instalment payment;
- complete HTTP flow from reservation through repayment history.

## Scope limitations

This MVP intentionally has no authentication/KYC, real payments, interest, late fees, partial repayments, refunds, inventory, credit scoring, or multi-currency support. The stable demo user is selected by application configuration. These exclusions keep the project focused on transactional correctness and concurrency.

The detailed design and Git milestone plan are in [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

