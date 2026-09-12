# NeighborLink 2.0

A community marketplace where members of a residential society lend and rent everyday items to each other — tools, camping gear, kitchen equipment, books. Listings are scoped to a society, so members rent from neighbours rather than strangers.

Built as a Spring Boot microservices backend with six services behind an API gateway, JWT authentication, role-based authorization, and a full rental-to-payment lifecycle including refunds.

> **Status:** Backend is complete and verified end to end by an automated smoke test. The React frontend is in progress.

---

## Architecture

```
                          ┌──────────────┐
                          │   Frontend   │
                          └──────┬───────┘
                                 │
                        ┌────────▼─────────┐
                        │   API Gateway    │  :8080
                        │  CORS · routing  │
                        └────────┬─────────┘
         ┌──────────┬────────────┼────────────┬──────────┐
         │          │            │            │          │
    ┌────▼───┐ ┌────▼───┐  ┌─────▼────┐ ┌─────▼────┐ ┌───▼────┐
    │  Auth  │ │  User  │  │ Society  │ │ Listing  │ │ Rental │
    │  :8081 │ │ :8082  │  │  :8083   │ │  :8084   │ │ :8085  │
    └────┬───┘ └────▲───┘  └──────────┘ └─────▲────┘ └──▲──┬───┘
         │          │                         │         │  │
         └──────────┘                         └─────────┘  │
          profile creation            ownership check      │
                                                    ┌──────▼──────┐
                                                    │   Payment   │
                                                    │    :8086    │
                                                    └─────────────┘
                                             confirm · fail · cancel · refund
```

Solid arrows are service-to-service calls authenticated with an internal service key. Every service owns its own Postgres database — there are no shared tables and no cross-service joins.

| Service | Port | Database | Gateway route | Responsibility |
|---|---|---|---|---|
| `api-gateway` | 8080 | — | — | Single entry point, routing, CORS |
| `auth-service` | 8081 | `auth_db` | `/auth/**` | Registration, login, JWT issue & refresh |
| `user-service` | 8082 | `user_db` | `/users/**` | User profiles |
| `society_service` | 8083 | `society_db` | `/societies/**` | Societies and membership |
| `listing-service` | 8084 | `listing_service` | `/listings/**` | Item listings, filtering, search |
| `rental_service` | 8085 | `rental_db` | `/rentals/**` | Bookings, availability, lifecycle |
| `payment_service` | 8086 | `payment_db` | `/payments/**` | Payments, settlement, refunds |

Every gateway route applies `StripPrefix=1`, and no controller declares a class-level path. So `POST /auth/login` at the gateway maps to `POST /login` in `AuthController`.

### Tech stack

Java 21 · Spring Boot 4.1 · Spring Cloud Gateway (WebMVC) · Spring Security 6 · Spring Data JPA · PostgreSQL · JJWT · Lombok · Maven

---

## Design decisions

The parts of this project that were actually interesting to build.

### A user cannot mark their own payment successful

The obvious way to let users pay is to expose a "mark successful" endpoint. That's also a way to let anyone confirm a rental they never paid for, straight from the browser console.

Instead, initiation and settlement are split:

- **`PUT /payments/{id}/initiate`** — callable by the payment's owner. Moves `CREATED → PENDING` and generates a provider reference. This is all the frontend can do.
- **Settlement** — reachable only through a provider callback authenticated by an internal service key. Moves `PENDING → SUCCESS | FAILED` and notifies the Rental Service.

A compromised frontend can start a payment it never completes, which is harmless. It cannot manufacture a success.

Because this project has no real payment provider, `PaymentSettlementSimulator` plays one: it schedules its own callback a few seconds after initiation, on a separate thread, so the renter's request returns immediately — exactly how a real gateway behaves. It is a labelled simulation behind a config flag, and the real callback endpoint sits beside it ready for a provider to be wired in.

```properties
payment.simulation.enabled=true          # false once a real provider is live
payment.simulation.delay-seconds=4
payment.simulation.failure-rate=0.0      # raise to exercise the failure path
```

### Cross-service calls fire after commit, not inline

The first version of the refund flow deadlocked.

Cancelling a rental should refund its payment. Doing that inline inside the rental transaction means: Rental sets `CANCELLED`, calls Payment Service over HTTP, Payment marks the payment `REFUNDED`, then Payment calls back into Rental — which blocks on a row lock held by the transaction that is itself blocked waiting for Payment's HTTP response. Both sides wait forever.

The fix is to publish a domain event and consume it with `@TransactionalEventListener(phase = AFTER_COMMIT)`. The rental row is durably committed before the outbound call happens, so the callback finds it already cancelled and returns idempotently, terminating the cycle.

```
user cancels rental
  → rental CANCELLED, committed
  → AFTER_COMMIT → payment refund
  → payment SUCCESS → REFUNDED          ✓ terminates

admin refunds payment
  → payment SUCCESS → REFUNDED
  → rental internal cancel → CANCELLED  ✓ terminates (no event published)
```

The same pattern drives payment settlement. See `RentalRefundListener` and `PaymentSettlementSimulator`.

### Two-layer authentication

Requests carry a JWT; services also talk to each other directly.

**User requests** — the JWT is validated independently by each service using a shared signing secret. The gateway does not terminate authentication, so no service trusts an unauthenticated caller even if something reaches it directly. Claims carry `sub` (user ID), `email`, and `role`.

**Service-to-service** — endpoints under `/internal/**` are guarded by an `X-Internal-Service-Key` header filter and bypass JWT entirely. They are reachable through the gateway but reject any request without the key, and never appear in the frontend API layer.

Two distinct keys are in use, so a leak in one direction does not compromise the other:

| Key | Protects | Used by |
|---|---|---|
| `USER_SERVICE_INTERNAL_KEY` | `POST /users/internal/profile` | Auth → User |
| `PAYMENT_INTERNAL_SERVICE_KEY` | `POST /payments/internal/**` | Rental → Payment |
| `RENTAL_INTERNAL_SERVICE_KEY` | `PUT /rentals/internal/**` | Payment → Rental |

### Registration is atomic across two services

Creating a user writes to `auth_db` and `user_db` — two databases, no distributed transaction available. If profile creation fails, the auth record is deleted and the request returns `503`, rather than leaving an account that can log in but has no profile.

### Listing filters use a JPA Specification

The original filter logic was an if/else chain over three optional parameters, which silently dropped the category filter when combined with a society. A `Specification` composes predicates independently, handling all combinations correctly and adding case-insensitive title and description search in the same place.

---

## Lifecycles

### Rental

```
POST /rentals ──► PAYMENT_PENDING
                       │
    payment SUCCESS ───┼──► CONFIRMED ──► ACTIVE ──► COMPLETED
                       │       (admin)     (admin)
    payment FAILED ────┼──► PAYMENT_FAILED
                       │         │
                       │         └──► retry payment ──► CONFIRMED
                       │
    user cancels ──────┴──► CANCELLED   (auto-refunds a settled payment)
```

`COMPLETED` is terminal — no transition leaves it, including admin overrides.

### Payment

```
CREATED ──initiate──► PENDING ──┬──► SUCCESS ──refund──► REFUNDED
 (owner)               (owner)  │
                                └──► FAILED ──initiate──► PENDING
```

Only a provider callback performs the `PENDING → SUCCESS | FAILED` transition. Settlement is idempotent, since real providers retry callbacks.

Cancelling a rental while its payment is still unsettled drives that payment to `FAILED`, so a later settlement attempt finds it non-`PENDING` and no-ops. Without this the payment would be orphaned in `PENDING` permanently.

---

## API reference

`public` = no token · `JWT` = bearer token required · `internal` = service key only, never from a browser

### Auth — `/auth`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register` | public | Create an account |
| `POST` | `/auth/login` | public | Returns access + refresh tokens, `userId`, `role` |
| `POST` | `/auth/refresh` | public | Rotates both tokens |
| `POST` | `/auth/logout` | public | Revokes the refresh token |
| `GET` | `/auth/admin/users` | ADMIN | List all accounts |

Access tokens live 15 minutes. Refresh tokens live 7 days, are stored SHA-256 hashed, and are single-use — rotated on refresh, revoked on logout.

### Users — `/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/users/{id}` | JWT · self or ADMIN | Full profile |
| `PUT` | `/users/{id}` | JWT · self or ADMIN | Update profile (full replace) |
| `POST` | `/users/batch` | JWT | Display names for a list of user IDs |
| `POST` | `/users/internal/profile` | internal | Called by Auth on registration |

`/users/batch` exposes only `userId`, `displayName`, and `profileImage`, so members can be rendered by name without exposing contact details.

### Societies — `/societies`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/societies` | public | All societies |
| `GET` | `/societies/{id}` | public | Society detail |
| `POST` | `/societies` | JWT | Create (creator becomes society admin) |
| `PUT` | `/societies/{id}` | JWT · society admin or ADMIN | Update |
| `DELETE` | `/societies/{id}` | ADMIN | Delete |
| `GET` | `/societies/user/{userId}` | JWT · self or ADMIN | A user's societies |
| `GET` | `/societies/{id}/members` | JWT | Member list |
| `POST` | `/societies/{id}/join` | JWT | Join as a member |
| `POST` | `/societies/{id}/members` | JWT · society admin or ADMIN | Add a member |
| `DELETE` | `/societies/{id}/members/{userId}` | JWT · self, society admin, or ADMIN | Remove or leave |

Society-scoped admin (`SocietyMemberRole.ADMIN`) is distinct from the platform-wide `ADMIN` role in the JWT.

### Listings — `/listings`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/listings` | public | Filter by `societyId`, `category`, `status`, `search` |
| `GET` | `/listings/categories` | public | Distinct categories, for filter dropdowns |
| `GET` | `/listings/{id}` | public | Listing detail |
| `POST` | `/listings` | JWT | Create (owner taken from the token) |
| `PUT` | `/listings/{id}` | JWT · owner or ADMIN | Update (full replace) |
| `DELETE` | `/listings/{id}` | JWT · owner or ADMIN | Delete |
| `GET` | `/listings/owner/{ownerId}` | JWT | A user's listings |

### Rentals — `/rentals`

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/rentals` | JWT | Book a listing for a date range |
| `GET` | `/rentals` | JWT | The caller's own rentals |
| `GET` | `/rentals/all` | ADMIN | All rentals, optional `?status=` |
| `GET` | `/rentals/{id}` | JWT · renter or ADMIN | Rental detail |
| `GET` | `/rentals/listing/{id}` | JWT · listing owner or ADMIN | Full rental history |
| `GET` | `/rentals/listing/{id}/availability` | JWT | Booked date ranges only |
| `PUT` | `/rentals/{id}/cancel` | JWT · renter or ADMIN | Cancel, auto-refunding if paid |
| `PUT` | `/rentals/{id}/complete` | ADMIN | Close a rental |
| `PUT` | `/rentals/{id}/status` | ADMIN | Override (rejects `COMPLETED`) |
| `PUT` | `/rentals/internal/{id}/confirm` | internal | Payment succeeded |
| `PUT` | `/rentals/internal/{id}/fail` | internal | Payment failed |
| `PUT` | `/rentals/internal/{id}/cancel` | internal | Payment refunded |

Booking validates that the listing is `ACTIVE`, rejects overlapping dates, computes `totalAmount` as `pricePerDay × days (inclusive)` server-side, and creates the payment record as a side effect. The amount is never accepted from the client.

The `/availability` split exists because the listing detail page needs booked dates, but exposing the full rental history would leak other members' identities and amounts to any authenticated user.

### Payments — `/payments`

| Method | Path | Auth | Description |
|---|---|---|---|
| `GET` | `/payments/my` | JWT | The caller's payments |
| `GET` | `/payments/{id}` | JWT · owner or ADMIN | Payment detail |
| `GET` | `/payments/rental/{rentalId}` | JWT · owner or ADMIN | Payments for a rental |
| `PUT` | `/payments/{id}/initiate` | JWT · owner | `CREATED`/`FAILED` → `PENDING` |
| `PUT` | `/payments/{id}/failed` | ADMIN | Override for a stuck payment |
| `PUT` | `/payments/{id}/refund` | ADMIN | `SUCCESS` → `REFUNDED`, cancels the rental |
| `POST` | `/payments/internal/provider/callback` | internal | Provider settlement |
| `POST` | `/payments/internal/rental/{id}/refund` | internal | Refund on cancellation |
| `POST` | `/payments/internal` | internal | Created by Rental on booking |

There is deliberately no user-facing endpoint that sets a payment to `SUCCESS`.

### Errors

Every service returns a consistent JSON body and maps exceptions to meaningful status codes — `400` validation, `401` unauthenticated, `403` unauthorized, `404` not found, `409` conflict or invalid state transition, `503` upstream failure. Spring MVC's own exceptions (unmatched route, wrong method, malformed body, bad path variable type) are handled explicitly rather than collapsing into `500`. No stack trace or internal path is ever returned to a client.

---

## Running locally

### Prerequisites

Java 21 · Maven · PostgreSQL 14+

### 1. Create the databases

```sql
CREATE DATABASE auth_db;
CREATE DATABASE user_db;
CREATE DATABASE society_db;
CREATE DATABASE listing_service;
CREATE DATABASE rental_db;
CREATE DATABASE payment_db;
```

Schemas are generated by Hibernate on first start.

### 2. Configure environment

Each service reads its configuration from a `.env` file in its own folder. These are gitignored. Copy `.env.example` in each service and fill it in.

Generate the secrets — the JWT secret must be **identical across all services**, while the three internal keys must each be **different**:

```bash
openssl rand -hex 32   # JWT_SECRET — same value everywhere
openssl rand -hex 32   # USER_SERVICE_INTERNAL_KEY
openssl rand -hex 32   # PAYMENT_INTERNAL_SERVICE_KEY
openssl rand -hex 32   # RENTAL_INTERNAL_SERVICE_KEY
```

Required variables per service:

| Service | Variables |
|---|---|
| `auth-service` | `DB_*`, `JWT_SECRET`, `JWT_EXPIRATION`, `JWT_REFRESH_EXPIRATION`, `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD`, `INITIAL_ADMIN_NAME`, `USER_SERVICE_URL`, `USER_SERVICE_INTERNAL_KEY` |
| `user-service` | `DB_*`, `JWT_SECRET`, `USER_SERVICE_INTERNAL_KEY` |
| `society_service` | `DB_*`, `JWT_SECRET` |
| `listing-service` | `DB_*`, `JWT_SECRET` |
| `rental_service` | `DB_*`, `JWT_SECRET`, `LISTING_SERVICE_URL`, `PAYMENT_SERVICE_URL`, `PAYMENT_INTERNAL_SERVICE_KEY`, `RENTAL_INTERNAL_SERVICE_KEY` |
| `payment_service` | `DB_*`, `JWT_SECRET`, `RENTAL_SERVICE_URL`, `RENTAL_INTERNAL_SERVICE_KEY`, `PAYMENT_INTERNAL_SERVICE_KEY` |

`auth-service` seeds a platform admin from `INITIAL_ADMIN_*` on first start.

### 3. Start the services

Order matters only for `auth-service`, which needs `user-service` reachable to create profiles. Start each from its own folder:

```bash
cd user-service     && ./mvnw spring-boot:run
cd auth-service     && ./mvnw spring-boot:run
cd society_service  && ./mvnw spring-boot:run
cd listing-service  && ./mvnw spring-boot:run
cd rental_service   && ./mvnw spring-boot:run
cd payment_service  && ./mvnw spring-boot:run
cd api-gateway      && ./mvnw spring-boot:run
```

Everything is then reachable at `http://localhost:8080`. Individual service ports should not be called directly.

### 4. Verify

```bash
python scripts/smoke-test.py
```

25 assertions covering registration, login, society creation, listing creation and filtering, booking with server-side amount calculation, payment initiation, asynchronous settlement, rental confirmation, cancellation with automatic refund, the cancel-while-pending race, cross-user authorization, and error status codes. A unique email is generated per run, so it is safe to repeat.

---

## Project structure

```
NeighborLink-2.0/
├── api-gateway/           routing + CORS
├── auth-service/          JWT, refresh rotation, admin seeding
├── user-service/          profiles
├── society_service/       societies, membership
├── listing-service/       listings, specification-based filtering
├── rental_service/        bookings, availability, lifecycle
├── payment_service/       payments, simulated provider, refunds
├── scripts/
│   └── smoke-test.py
└── CLAUDE.md
```

Each service follows the same layout:

```
src/main/java/com/neighborlink/<service>/
├── controller/     HTTP layer, no business logic
├── service/        business rules, transactions, outbound clients
├── repository/     Spring Data JPA
├── entity/         JPA entities
├── dto/            request and response records
├── security/       JWT filter, internal key filter, security config
├── exception/       domain exceptions + GlobalExceptionHandler
└── event/          domain events for AFTER_COMMIT listeners
```

---

## Security notes

- Passwords hashed with BCrypt; never logged or returned.
- Refresh tokens stored SHA-256 hashed, single-use, revoked on logout.
- Rental amounts computed server-side from the listing price — never accepted from the client.
- Payment success reachable only through a provider callback, never a user request.
- Each service validates the JWT independently; the gateway is not a trust boundary.
- `/internal/**` endpoints guarded by service keys with no JWT path.
- All secrets in gitignored `.env` files. No credentials in source or in commit history.

---

## Roadmap

- React + TypeScript frontend (in progress)
- Redis caching on listing reads, with eviction owned by the writing service
- Real payment provider replacing the simulator
- Pagination on listings and rentals
- Review service and notification service
- Docker Compose for one-command startup
