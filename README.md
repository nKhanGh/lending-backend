# Mini Lending Credit Line Backend

Java 21 / Spring Boot 3.5 / PostgreSQL simulator for credit assessment, limit utilization and principal repayment. This is a learning project: there are no real loans, disbursements or payments.

## Run locally

Requirements: Java 21, Maven 3.9+, Docker Desktop.

```bash
docker compose up -d postgres
mvn spring-boot:run
```

PostgreSQL is exposed on host port `55433`. The new Compose database and volume are separate from the previous PayLater database. Environment overrides are listed in `.env.example`. Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html); health: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

## Demo (PowerShell)

```powershell
$base = 'http://localhost:8080/api'
$application = Invoke-RestMethod "$base/credit-applications" -Method Post -ContentType 'application/json' -Body '{"monthlyNetIncome":3000.00,"monthlyLivingExpenses":1400.00,"monthlyDebtPayments":500.00}'
$account = Invoke-RestMethod "$base/credit-account"
$drawdown = Invoke-RestMethod "$base/credit-drawdowns" -Method Post -ContentType 'application/json' -Headers @{'Idempotency-Key'='demo-draw-800'} -Body '{"amount":800.00}'
$id = $drawdown.result.drawdownId
$firstInstalment = $drawdown.result.instalments[0].id
$repayment = Invoke-RestMethod "$base/instalments/$firstInstalment/repay" -Method Post -Headers @{'Idempotency-Key'='demo-repay-1'}
Invoke-RestMethod "$base/credit-drawdowns/$id"
Invoke-RestMethod "$base/credit-drawdowns/$id/repayments"
```

The demo decision is `APPROVED_TIER_1000`: surplus SGD 1100 and limit SGD 1000. A drawdown of SGD 800 produces four SGD 200 instalments; after the first repayment, used limit is SGD 600 and available limit is SGD 400. Pay the remaining instalments in sequence to reach `PAID_OFF` and restore SGD 1000 available. A successful application is accepted once per seeded user. To repeat the demo from the beginning, use a new empty database/volume; do not alter an existing Flyway migration or erase data you want to keep.

## API

| Method | Path | Notes |
|---|---|---|
| POST | `/api/credit-applications` | Income, living expenses, debt payments; optional `currency=SGD` |
| GET | `/api/credit-applications/latest` | Most recent decision |
| GET | `/api/credit-account` | Total, used and available limit after approval |
| POST | `/api/credit-drawdowns` | `{ "amount": 800.00 }`; requires `Idempotency-Key` |
| GET | `/api/credit-drawdowns` | Drawdown list |
| GET | `/api/credit-drawdowns/{id}` | Detail and four instalments |
| POST | `/api/instalments/{id}/repay` | Next unpaid instalment only; requires `Idempotency-Key` |
| GET | `/api/credit-drawdowns/{id}/repayments` | Successful repayment history |

All results are wrapped in the existing `ApiResponse<T>` shape (`code`, `message`, `result`, `timestamp`). For one operation, retry with the same key and body to get the original result. Use a new key for a new operation.

The smallest accepted drawdown is SGD 0.04 because the schema requires four positive instalments in cents. Monthly dates use UTC. The `DEMO_V1` policy and data model are detailed in [IMPLEMENTATION_PLAN.md](IMPLEMENTATION_PLAN.md).

## Tests and Flyway

```bash
mvn clean verify
```

Integration tests start an ephemeral PostgreSQL 17 instance through Testcontainers. Flyway runs `V1` (tables/constraints) and `V2` (demo user) at startup, then records checksums in `flyway_schema_history`. Hibernate validates the resulting schema. Future schema changes require a new numbered migration; the previous PayLater volume is intentionally kept separate.
