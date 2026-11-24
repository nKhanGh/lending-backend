# Mini Lending Credit Line Backend

Java 21, Spring Boot 3.5 and PostgreSQL simulator for credit assessment,
limit utilization and repayment. No real loans, disbursements or payments.

## Run locally

Requirements: Java 21, Maven 3.9+, Docker Desktop.

```powershell
docker compose up -d postgres
mvn spring-boot:run
```

PostgreSQL is exposed on port `55433`. Flyway applies V1 through V4 on startup
and Hibernate validates the schema. Existing migrations must not be edited.

Set `JWT_SECRET` to a random value of at least 32 bytes outside the repository
if tokens should survive restarts. Without it, a random local-only key is
generated at startup. Optionally set `BOOTSTRAP_ADMIN_EMAIL` and
`BOOTSTRAP_ADMIN_PASSWORD` (12+ characters) to create an admin at first start.
No admin credentials are committed to the repository.

## Accounts and roles

```powershell
$base = 'http://localhost:8080/api'
$register = Invoke-RestMethod "$base/auth/register" -Method Post -ContentType 'application/json' -Body '{"displayName":"Demo","email":"demo@example.test","password":"password123"}'
$token = $register.result.accessToken
$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod "$base/auth/me" -Headers $headers
```

`POST /api/auth/login` issues another one-hour bearer token. Customer routes
are scoped to the token user. `/api/admin/**` requires the ADMIN role. The
original seeded demo user is retained for migration compatibility but has no
login credential. Register a new user to use V2.

The existing frontend branch has no login flow and is not yet compatible with
this backend branch.

## Demo credit flow

```powershell
$application = Invoke-RestMethod "$base/credit-applications" -Method Post -ContentType 'application/json' -Headers $headers -Body '{"monthlyNetIncome":3000.00,"monthlyLivingExpenses":1400.00,"monthlyDebtPayments":500.00}'
$drawHeaders = @{ Authorization = "Bearer $token"; 'Idempotency-Key' = 'demo-draw-800' }
$drawdown = Invoke-RestMethod "$base/credit-drawdowns" -Method Post -ContentType 'application/json' -Headers $drawHeaders -Body '{"amount":800.00}'
$instalmentId = $drawdown.result.instalments[0].id
$payHeaders = @{ Authorization = "Bearer $token"; 'Idempotency-Key' = 'demo-payment-1' }
$attempt = Invoke-RestMethod "$base/instalments/$instalmentId/repay" -Method Post -Headers $payHeaders
```

The payment attempt returns HTTP 202 and `PENDING`. It does not change the
balance. To simulate an external payment result, log in as the configured admin:

```powershell
$adminLogin = Invoke-RestMethod "$base/auth/login" -Method Post -ContentType 'application/json' -Body '{"email":"admin@example.test","password":"your-admin-password"}'
$adminHeaders = @{ Authorization = "Bearer $($adminLogin.result.accessToken)" }
$attemptId = $attempt.result.paymentAttemptId
Invoke-RestMethod "$base/admin/payment-attempts/$attemptId/settle" -Method Post -Headers $adminHeaders -ContentType 'application/json' -Body '{"outcome":"SUCCEEDED","providerEventId":"demo-event-1"}'
```

Only `SUCCEEDED` marks the instalment paid, reduces principal and restores
available credit. `FAILED` leaves balances untouched and permits a new
attempt. Replaying the same idempotency key or provider event is safe.
Pass optional JSON `{"amount":40.00}` to pay part of the earliest unpaid
instalment. Omit the body to request its full remaining balance. Partial
payments reduce principal immediately after settlement; the instalment stays
open until fully paid.

At 00:05 UTC each day, the overdue job marks unpaid instalments past their
due date as `OVERDUE`. Drawdown detail also reports `daysOverdue` and
the remaining amount, including after a partial payment. A fully settled
instalment changes to `PAID`.

## API

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register`, `/api/auth/login` | Customer signup and login |
| GET | `/api/auth/me` | Current user |
| POST | `/api/credit-applications` | Assess income and assign limit |
| GET | `/api/credit-applications/latest`, `/api/credit-account` | Current decision and balance |
| POST | `/api/credit-drawdowns` | Use limit; requires Idempotency-Key |
| GET | `/api/credit-drawdowns`, `/api/credit-drawdowns/{id}` | Drawdowns and instalments |
| POST | `/api/instalments/{id}/repay` | Create PENDING payment attempt; optional partial amount |
| GET | `/api/payment-attempts/{id}` | Own payment status |
| GET | `/api/credit-drawdowns/{id}/payment-attempts` | Own attempt history |
| GET | `/api/credit-drawdowns/{id}/repayments` | Successful repayments |
| POST | `/api/admin/payment-attempts/{id}/settle` | Simulate provider outcome |
| GET | `/api/admin/users` | Admin-only user listing |

All responses use `ApiResponse<T>`. Swagger UI is at
[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
The health endpoint is at [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health).

## Verify

```bash
mvn clean verify
```

Integration tests run against ephemeral PostgreSQL 17 with Testcontainers.
They cover user isolation, retry, concurrency, payment success and failure.
