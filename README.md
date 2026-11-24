# Mini Lending — Credit Line MVP backend

Backend implementation of the user-provided `mini-lending-credit-line-30h.md` proof of concept. It uses one seeded demo user, one possible credit account, SGD amounts, 0% interest and fees, and simulated repayments. No real loan or payment is created.

## Run

Requirements: Java 21, Maven 3.9+, Docker Desktop with a running Linux engine.

```powershell
docker compose up -d postgres
mvn spring-boot:run
```

PostgreSQL listens on `localhost:55432` by default; the API listens on `localhost:8080`. Copy `.env.example` values into environment variables if you need another port or credentials. Flyway applies the six-table schema and seeds the demo user automatically. Swagger UI: `http://localhost:8080/swagger-ui.html`.

To rerun the demonstration from an empty state, stop the application and delete the Docker Compose database volume with `docker compose down -v`, then start PostgreSQL and the application again. This deletes all demo data in that Compose volume.

## API

All success responses use the existing `ApiResponse<T>` envelope with the payload in `result`. Errors use the same envelope and HTTP `400`, `404`, or `409`.

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/credit-applications` | Assess input; create account only if approved |
| `GET` | `/api/credit-applications/latest` | Latest decision |
| `GET` | `/api/credit-account` | Total, used, available limit |
| `POST` | `/api/credit-drawdowns` | Use limit; `Idempotency-Key` required |
| `GET` | `/api/credit-drawdowns` | List drawdowns |
| `GET` | `/api/credit-drawdowns/{id}` | Drawdown and four instalments |
| `POST` | `/api/instalments/{id}/repay` | Pay the earliest unpaid instalment in full; `Idempotency-Key` required |
| `GET` | `/api/credit-drawdowns/{id}/repayments` | Simulated repayment history |

Example requests:

```http
POST /api/credit-applications
Content-Type: application/json

{"monthlyNetIncome":3000.00,"monthlyLivingExpenses":1400.00,"monthlyDebtPayments":500.00}
```

The `DEMO_V1` policy calculates a surplus of SGD 1100 and approves a limit of SGD 1000. Once approved, the demo user cannot submit another application until the database is reset. A rejected application can be resubmitted.

```http
POST /api/credit-drawdowns
Idempotency-Key: draw-001
Content-Type: application/json

{"amount":800.00}
```

That creates four monthly SGD 200 instalments and leaves SGD 200 available. Read the drawdown detail to get the first instalment UUID, then call:

```http
POST /api/instalments/{first-instalment-uuid}/repay
Idempotency-Key: repay-001
```

The outstanding principal becomes SGD 600 and available limit becomes SGD 400. Paying the remaining three instalments closes the drawdown and restores the full SGD 1000 limit. A repeated key with the same operation and payload returns the existing record; reusing it for different input returns `409`.

For new operations, drawdown and repayment responses store the balance at the time of success. Retrying later returns that original result even if the current account balance has changed. Flyway V2 adds these snapshots without deleting V1 data. Operations created before V2 have no historical snapshot, so their retry response uses the current available balance.

Drawdown amounts must be at least SGD 0.04 so that all four instalments can be positive. The first three amounts are rounded down to cents and the last receives the remainder. All balance mutations occur in one PostgreSQL transaction under a pessimistic lock on the credit account row.

## Test

```powershell
mvn clean verify
```

The integration tests start PostgreSQL through Testcontainers. They cover the decision boundaries, limit rejection, concurrent drawdowns and repayments, exact retry responses, repayment order, closure, and transaction rollback. See [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md) for the backend scope.
