# Finance Dashboard Backend

A production-structured REST API for a multi-role finance dashboard, built with **Java 21 + Spring Boot 3.2**.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Default Credentials](#default-credentials)
- [Project Structure](#project-structure)
- [API Reference](#api-reference)
- [Role-Based Access Control](#role-based-access-control)
- [Design Decisions & Tradeoffs](#design-decisions--tradeoffs)
- [Assumptions](#assumptions)
- [Running Tests](#running-tests)
- [Switching to PostgreSQL](#switching-to-postgresql)

---

## Tech Stack

| Concern            | Choice                        | Reason                                              |
|--------------------|-------------------------------|-----------------------------------------------------|
| Language           | Java 21                       | LTS release, records, pattern matching              |
| Framework          | Spring Boot 3.2               | Mature ecosystem, fast DI, great test tooling       |
| Database           | H2 (file-mode)                | Zero setup, SQLite-like persistence, easy to swap   |
| ORM                | Spring Data JPA + Hibernate   | Eliminates boilerplate, handles auditing            |
| Auth               | JWT (HS256 via jjwt)          | Stateless, scales horizontally, no session storage  |
| RBAC               | Spring `@PreAuthorize`        | Method-level, co-located with logic it protects     |
| Validation         | Bean Validation (JSR-380)     | Declarative, composable, framework-standard         |
| API Docs           | SpringDoc OpenAPI (Swagger)   | Auto-generated, always in sync with code            |
| Boilerplate        | Lombok                        | Eliminates getters/setters/builders                 |
| Tests              | JUnit 5 + Mockito + MockMvc   | Unit + integration coverage                         |

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+

### Run

```bash
git clone <repository-url>
cd finance-backend
mvn spring-boot:run
```

The server starts on **http://localhost:8080**.

### Useful URLs

| URL                                   | Description                         |
|---------------------------------------|-------------------------------------|
| http://localhost:8080/swagger-ui.html | Interactive API documentation       |
| http://localhost:8080/h2-console      | H2 database browser (dev only)      |
| http://localhost:8080/api-docs        | Raw OpenAPI JSON spec               |

---

## Default Credentials

Seeded automatically on first startup via `data.sql`:

| Role    | Email                   | Password      |
|---------|-------------------------|---------------|
| ADMIN   | admin@finance.com       | `Admin@123`   |
| ANALYST | analyst@finance.com     | `Analyst@123` |
| VIEWER  | viewer@finance.com      | `Viewer@123`  |

### Authentication Flow

```bash
# 1. Login to get a token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@finance.com","password":"Admin@123"}'

# Response:
# { "token": "eyJhbGci...", "tokenType": "Bearer", "role": "ADMIN", ... }

# 2. Use the token on protected endpoints
curl http://localhost:8080/api/records \
  -H "Authorization: Bearer eyJhbGci..."
```

Or use **Swagger UI**: click "Authorize" at the top right and paste your token.

---

## Project Structure

```
src/main/java/com/finance/
│
├── config/
│   ├── SecurityConfig.java       # Spring Security, JWT filter wiring, CORS
│   └── OpenApiConfig.java        # Swagger UI configuration
│
├── controller/                   # HTTP layer — thin adapters only
│   ├── AuthController.java
│   ├── UserController.java
│   ├── FinancialRecordController.java
│   └── DashboardController.java
│
├── service/                      # Business logic + RBAC enforcement
│   ├── AuthService.java
│   ├── UserService.java
│   ├── FinancialRecordService.java
│   └── DashboardService.java
│
├── repository/                   # Spring Data JPA interfaces + JPQL queries
│   ├── UserRepository.java
│   └── FinancialRecordRepository.java
│
├── model/                        # JPA entities + enums
│   ├── User.java
│   ├── Role.java                 # VIEWER | ANALYST | ADMIN
│   ├── UserStatus.java           # ACTIVE | INACTIVE
│   ├── FinancialRecord.java
│   └── RecordType.java           # INCOME | EXPENSE
│
├── dto/
│   ├── request/                  # Validated inbound payloads
│   │   ├── LoginRequest.java
│   │   ├── CreateUserRequest.java
│   │   ├── UpdateUserRequest.java
│   │   ├── CreateRecordRequest.java
│   │   └── UpdateRecordRequest.java
│   └── response/                 # Outbound response shapes
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── FinancialRecordResponse.java
│       ├── DashboardSummaryResponse.java
│       └── PagedResponse.java    # Generic pagination envelope
│
├── security/
│   ├── JwtUtil.java              # Token generation + validation
│   ├── JwtAuthenticationFilter.java  # Intercepts every request
│   ├── UserPrincipal.java        # Spring Security UserDetails adapter
│   └── CustomUserDetailsService.java
│
└── exception/
    ├── GlobalExceptionHandler.java    # @RestControllerAdvice — unified error format
    ├── ResourceNotFoundException.java # 404
    ├── DuplicateResourceException.java# 409
    └── BusinessException.java         # 400 (rule violation)
```

---

## API Reference

All protected endpoints require: `Authorization: Bearer <token>`

### Authentication

| Method | Endpoint          | Auth | Description       |
|--------|-------------------|------|-------------------|
| POST   | `/api/auth/login` | ❌   | Login, get token  |

### Users

| Method | Endpoint         | Role  | Description              |
|--------|------------------|-------|--------------------------|
| GET    | `/api/users`     | ADMIN | List users (paginated)   |
| POST   | `/api/users`     | ADMIN | Create new user          |
| GET    | `/api/users/me`  | ANY   | Own profile              |
| GET    | `/api/users/{id}`| ADMIN | Get user by ID           |
| PATCH  | `/api/users/{id}`| ADMIN | Partial update user      |
| DELETE | `/api/users/{id}`| ADMIN | Soft-delete user         |

**Query params for GET `/api/users`:**
```
role=ADMIN|ANALYST|VIEWER    (optional filter)
status=ACTIVE|INACTIVE       (optional filter)
page=0                       (0-indexed, default 0)
size=20                      (default 20, max 100)
```

### Financial Records

| Method | Endpoint            | Role  | Description                  |
|--------|---------------------|-------|------------------------------|
| GET    | `/api/records`      | ANY   | List records (filtered, paged)|
| POST   | `/api/records`      | ADMIN | Create record                |
| GET    | `/api/records/{id}` | ANY   | Get single record            |
| PATCH  | `/api/records/{id}` | ADMIN | Partial update               |
| DELETE | `/api/records/{id}` | ADMIN | Soft-delete                  |

**Query params for GET `/api/records`:**
```
type=INCOME|EXPENSE          (optional)
category=Salary              (case-insensitive, optional)
from=2024-01-01              (yyyy-MM-dd, optional)
to=2024-03-31                (yyyy-MM-dd, optional)
page=0
size=20
```

**Create/Update record body:**
```json
{
  "amount": 5000.00,
  "type": "INCOME",
  "category": "Salary",
  "recordDate": "2024-03-31",
  "description": "March salary"
}
```

### Dashboard

| Method | Endpoint                  | Role            | Description                                    |
|--------|---------------------------|-----------------|------------------------------------------------|
| GET    | `/api/dashboard/summary`  | ANALYST, ADMIN  | Full summary: totals, categories, trends       |
| GET    | `/api/dashboard/basic`    | ANY             | Basic summary: totals + net balance only       |

Both accept optional `from` and `to` date range query params.

**Full summary response shape:**
```json
{
  "totalIncome": 195000.00,
  "totalExpenses": 14350.00,
  "netBalance": 180650.00,
  "incomeByCateogry": [
    { "category": "Salary", "total": 150000.00 },
    { "category": "Freelance", "total": 30000.00 }
  ],
  "expensesByCategory": [
    { "category": "Rent", "total": 2400.00 }
  ],
  "monthlyTrends": [
    { "year": 2024, "month": 1, "monthLabel": "Jan 2024",
      "income": 50000.00, "expenses": 0.00, "net": 50000.00 }
  ],
  "recentActivity": [ ... ]
}
```

### Error Response Format

All errors return a consistent envelope:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "FinancialRecord not found with id: 99",
  "timestamp": "2024-03-15T10:30:00"
}
```

Validation errors include field-level details:
```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "One or more fields are invalid",
  "fieldErrors": {
    "amount": "Amount must be greater than zero",
    "recordDate": "Record date cannot be in the future"
  }
}
```

---

## Role-Based Access Control

```
                    VIEWER    ANALYST    ADMIN
─────────────────────────────────────────────
Login                 Yes        Yes        Yes
Own profile           Yes        Yes        Yes
View records          Yes        Yes        Yes
Basic dashboard       Yes        Yes        Yes
Full dashboard         No        Yes        Yes
Create records         No         No        Yes
Update records         No         No        Yes
Delete records         No         No        Yes
List users             No         No        Yes
Create/update users    No         No        Yes
Delete users           No         No        Yes
```

RBAC is enforced at the **service layer** using Spring's `@PreAuthorize` annotations. 
This means the rules hold even if the service is called from a scheduled job or another service, 
not just through HTTP controllers.

---

## Design Decisions & Tradeoffs

### 1. H2 in File Mode vs PostgreSQL
- **Chosen:** H2 file-mode (`jdbc:h2:file:./data/financedb`)
- **Why:** Zero setup — clone and run. Data persists between restarts.
- **Tradeoff:** Not production-grade. H2 lacks advanced features (full-text search, window functions, read replicas).
- **Migration path:** Change 3 lines in `application.yml` — datasource URL, driver, dialect. No Java code changes required.

### 2. JWT (Stateless) vs Sessions
- **Chosen:** Stateless JWT, 24-hour expiry, HS256 signing
- **Why:** No shared session store needed; scales to multiple instances trivially.
- **Tradeoff:** Tokens cannot be revoked before expiry. If a user is deactivated, their existing token remains valid until expiry.
- **Mitigation applied:** The `JwtAuthenticationFilter` reloads `UserDetails` from DB on every request, so INACTIVE users are rejected immediately even with a valid token.
- **Alternative for immediate revocation:** Maintain a Redis blacklist of invalidated JWIs (token IDs).

### 3. `@PreAuthorize` (Method Security) vs Route-Level Security
- **Chosen:** Method-level `@PreAuthorize` in service classes
- **Why:** Rules are co-located with the logic they protect. Hard to accidentally bypass by adding a new route.
- **Tradeoff:** Harder to audit all rules at once (must grep codebase vs. a single policy file).
- **Alternative:** Spring Security's `authorizeHttpRequests()` in `SecurityConfig` — centralized but only enforced at the HTTP layer.

### 4. Soft Delete vs Hard Delete
- **Chosen:** Soft delete (`deleted_at` timestamp)
- **Why:** Preserves audit trail. A deleted user's financial records still show "created by" correctly. Supports potential future "restore" feature.
- **Tradeoff:** All queries must filter `deleted_at IS NULL`. Forgotten filter = data leak of deleted records.
- **Mitigation applied:** Custom `findActive*` repository methods so callers always get the filtered versions.

### 5. BigDecimal for Monetary Amounts
- **Chosen:** `BigDecimal` with `DECIMAL(19,4)` in DB
- **Why:** Floating-point types (`double`, `float`) cannot represent most decimal fractions exactly. `0.1 + 0.2 != 0.3` in IEEE 754. Financial software must use exact decimal arithmetic.
- **Tradeoff:** Slightly more verbose code; `BigDecimal` operations are explicit.

### 6. JPQL Aggregations vs In-Memory Computation
- **Chosen:** JPQL `SUM`, `GROUP BY` pushed to the database
- **Why:** The DB processes aggregations orders of magnitude faster than loading millions of rows into Java heap and summing them.
- **Tradeoff:** JPQL is less expressive than SQL (no window functions, limited CTE support).
- **Future path:** For complex analytics, use `@NativeQuery` or a dedicated analytics layer (ClickHouse, TimescaleDB).

### 7. Category as Free-Form String vs Category Table
- **Chosen:** `category VARCHAR(100)` on the record
- **Why:** Simpler schema, faster iteration, no foreign key overhead for a dashboard MVP.
- **Tradeoff:** No enforced taxonomy — "salary", "Salary", "SALARY" are treated as different categories (mitigated by case-insensitive filter in the query).
- **Production recommendation:** Add a `categories` lookup table with a foreign key on `financial_records`.

### 8. Single Role Per User vs Permission Sets
- **Chosen:** One `role` enum field per user
- **Why:** The assignment defines three clear roles with well-defined boundaries. A roles table adds schema complexity without benefit at this scope.
- **Tradeoff:** Cannot mix permissions (e.g. "ANALYST who can also create records"). For more granular control, a `user_permissions` join table would be needed.

---

## Assumptions

1. **Financial records are organization-wide**, not per-user. Any VIEWER can see all records.
2. **Categories are free-form text** — not from a predefined list.
3. **Amounts are always positive**; the `type` field (INCOME/EXPENSE) determines direction.
4. **Record dates can be past or present**, not future — enforced by `@PastOrPresent`.
5. **Emails are unique across all users**, including soft-deleted ones — prevents reuse of a deactivated account's email.
6. **At least one ADMIN must always exist** — the service blocks operations that would violate this invariant.
7. **Password changes are out of scope** — not specified in the assignment; adding a `PATCH /api/users/me/password` endpoint would be straightforward.

---

## Running Tests

```bash
# All tests (unit + integration)
mvn test

# Only unit tests (fast)
mvn test -Dtest="*ServiceTest"

# Only integration tests
mvn test -Dtest="IntegrationTest"

# With coverage report (target/site/jacoco/index.html)
mvn verify
```

Tests use an **in-memory H2 database** (separate from dev data) and the `test` Spring profile, so they never affect your development database.

---

## Switching to PostgreSQL

1. Add the PostgreSQL driver to `pom.xml`:
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

2. Update `application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/financedb
    driver-class-name: org.postgresql.Driver
    username: your_user
    password: your_password
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
```

3. Update `data.sql` — replace `MERGE INTO ... KEY(...)` with:
```sql
INSERT INTO users (...) VALUES (...) ON CONFLICT (email) DO NOTHING;
```

No Java code changes required.

---

## Future Improvements

- **Token refresh endpoint** — `/api/auth/refresh` to extend sessions without re-login
- **Password change endpoint** — `PATCH /api/users/me/password`
- **Category management** — lookup table with CRUD endpoints
- **Export** — CSV/PDF export of filtered records (Analyst + Admin)
- **Rate limiting** — Bucket4j or Spring's built-in rate limiter on `/api/auth/login`
- **Audit log table** — record every mutation with who, what, when
- **Redis token blacklist** — for immediate token revocation on logout/deactivation
