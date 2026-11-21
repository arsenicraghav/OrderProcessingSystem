# Order Processing Service

![CI](https://github.com/arsenicraghav/OrderProcessingSystem/actions/workflows/ci.yml/badge.svg)

**Live demo:** _add your Render URL here_ (free tier: first request after idle takes ~1 min)

Spring Boot 3.3 / Java 17+ / JPA + SQL, structured around classic design patterns.
Databases: **H2** (default, zero setup), **PostgreSQL** (`postgres` profile, used in production),
**MySQL 8** (`mysql` profile). The schema is managed by **Flyway** with one SQL dialect per database.
Messaging: **Apache ActiveMQ Artemis 2.57.0** (JMS), embedded by default or an external broker.

## Run

**Without containers** (H2 in memory + embedded Artemis; nothing to install):
```bash
mvn spring-boot:run
```

**App on your machine, PostgreSQL + Artemis in Docker:**
```bash
docker compose up -d postgres artemis
SPRING_PROFILES_ACTIVE=postgres ARTEMIS_MODE=native \
SPRING_ARTEMIS_USER=artemis SPRING_ARTEMIS_PASSWORD=artemis mvn spring-boot:run
```

**Everything in containers** (app + PostgreSQL + Artemis):
```bash
docker compose up --build        # Ctrl+C to stop; `docker compose down -v` also wipes the database
```

**Throwaway PostgreSQL via Testcontainers** (no compose): `mvn spring-boot:test-run`

**End-to-end check of a running app:** `./scripts/e2e.sh` (every requirement, PASS/FAIL per step; with the compose stack it also checks Artemis and PostgreSQL)

**Tests:** `mvn test` (no Docker) · `mvn verify` (adds PostgreSQL + Artemis containers)

URLs: API docs http://localhost:8080 (redirects to Swagger UI) · health http://localhost:8080/actuator/health ·
Artemis console http://localhost:8161 (artemis/artemis, container modes) · H2 console http://localhost:8080/h2-console (H2 mode)

### Database configuration

| Setting | How |
|---|---|
| Hosted Postgres (Render, Neon, Supabase, Railway) | `SPRING_PROFILES_ACTIVE=postgres` + `DATABASE_URL=postgresql://user:pass@host:5432/db[?sslmode=require]` |
| Any database via JDBC | `DB_URL=jdbc:postgresql://…` + `DB_USER` + `DB_PASSWORD` with the matching profile |
| Migrations | `src/main/resources/db/migration/{h2,postgresql,mysql}/V1__create_orders.sql`, applied automatically at startup |

`DatabaseUrlEnvironmentPostProcessor` (an Adapter) converts the platform-style `DATABASE_URL` into
JDBC settings, keeping query options like `sslmode=require`.

## Deploy (demo)

The repo includes a `Dockerfile`, a Render blueprint (`render.yaml`) and a GitHub Actions build
(`.github/workflows/ci.yml`) that runs every test, including the real-PostgreSQL one, on each push.

**Render (free web service + free Postgres):**
1. Push the repo to GitHub.
2. render.com → **New → Blueprint** → pick the repo. Render creates `orders-db` (Postgres, private network only)
   and `order-service` (Docker), and wires `DATABASE_URL` + `SPRING_PROFILES_ACTIVE=postgres` automatically.
3. Open the service URL: `/` redirects to Swagger UI; `/actuator/health` is the health check (includes the database).

Free-tier limits: the web service sleeps after 15 minutes idle (about a minute to wake), and Render's free Postgres
**expires 30 days after creation** (1 GB, no backups, one per workspace). For a longer-lived free database,
create one on Neon or Supabase and set `DATABASE_URL` on the web service to its connection string instead.

```bash
docker build -t order-service . && docker run -p 8080:8080 order-service   # run the container locally (H2)
```

## API

| Method | Path | Purpose |
|---|---|---|
| POST  | `/api/orders` | Create an order with multiple items → `201` + `Location` |
| GET   | `/api/orders/{id}` | Order details with items, `cancellable`, `allowedNextStatuses` |
| GET   | `/api/orders?status=PENDING&customerId=c-1&page=0&size=20` | List (newest first), optional filters, paged |
| PATCH | `/api/orders/{id}/status` | `{"status":"SHIPPED"}` – lifecycle-checked transition |
| POST  | `/api/orders/{id}/cancel` | Cancel; only while `PENDING` |

```bash
curl -s -XPOST localhost:8080/api/orders -H 'Content-Type: application/json' -d '{
  "customerId": "cust-42",
  "items": [
    {"productId":"sku-1","productName":"Keyboard","quantity":2,"unitPrice":49.99},
    {"productId":"sku-2","productName":"Mouse","quantity":1,"unitPrice":20.02}
  ]}'
curl -s localhost:8080/api/orders/1
curl -s 'localhost:8080/api/orders?status=PENDING'
curl -s -XPOST localhost:8080/api/orders/1/cancel
curl -s -XPATCH localhost:8080/api/orders/1/status -H 'Content-Type: application/json' -d '{"status":"PROCESSING"}'
```

Errors are RFC 7807 `application/problem+json`: `400` validation / business-rule rejection,
`404` unknown order, `409` illegal transition, cancel of a non-PENDING order, or concurrent modification.

## Request flow

```
HTTP ─► OrderController ─► OrderFacade ─┬─► CommandBus (LoggingCommandBus ─► SimpleCommandBus) ─► *Handler
                                        │        handlers use OrderFactory (validation chain + pricing + builder),
                                        │        Order/OrderState, OrderRepository, and publish domain events
                                        └─► OrderQueryService ─► OrderRepository + OrderSpecifications
Domain events ─► OrderAuditListener, CustomerNotificationListener ─► NotificationChannel
              └─► JmsOrderEventPublisher ─► Artemis queue orders.events ─► other systems
Artemis queue orders.status-updates ─► OrderStatusUpdateListener ─► CommandBus ─► ChangeOrderStatusHandler
@Scheduled PendingOrderProcessor (AbstractScheduledJob) ─► CommandBus ─► PromotePendingOrdersHandler
```

## Design patterns

| Pattern | Where | Why |
|---|---|---|
| **State** | `OrderState`, `PendingState`, `ProcessingState`, `ShippedState`, `DeliveredState`, `CancelledState` | Each status owns its rules; `Order.transitionTo()` / `cancel()` delegate to it. Only `PendingState` allows cancel. |
| **Singleton** | State `INSTANCE`s; all Spring beans | States are stateless, so one shared instance each. |
| **Factory** | `OrderStateFactory`, `OrderFactory`, `PricingStrategyFactory` | One place to map status → state, request → valid priced order, config name → strategy. |
| **Builder** | `Order.Builder` | Assembles a PENDING order with items and computes the total in one step; rejects incomplete orders. |
| **Strategy** | `PricingStrategy` (`standard`, `bulk-discount`); `NotificationChannel` | Swap pricing via `orders.pricing.strategy`, or the notification channel by swapping a bean. |
| **Chain of Responsibility** | `OrderValidator` → `NonEmptyOrderValidator` → `DuplicateProductValidator` → `MaxOrderValueValidator` | Each business rule is one link; add a rule by adding a bean with `@Order`. |
| **Command** | `CreateOrderCommand`, `ChangeOrderStatusCommand`, `CancelOrderCommand`, `PromotePendingOrdersCommand` | Every write is an immutable request object with its own handler. |
| **Mediator** | `CommandBus` / `SimpleCommandBus` | Controller and job send commands without knowing the handlers. |
| **Decorator** | `LoggingCommandBus` (`@Primary`) wraps `SimpleCommandBus` | Adds timing/logging; metrics or retries could be stacked the same way. |
| **Template Method** | `AbstractStatusChangeHandler` (load → change → flush/version check → publish event); `AbstractScheduledJob` (time → run → log, never crash the schedule) | Fixed algorithm in the base class, only the varying step in subclasses. |
| **Observer** | Domain events + `OrderAuditListener`, `CustomerNotificationListener` (`@TransactionalEventListener`) | Side effects are decoupled and run only after commit. |
| **Facade** | `OrderFacade` | Controller sees five simple methods instead of bus + queries + mapper. |
| **Repository** | `OrderRepository` | Collection-like persistence; the bulk SQL update lives here. |
| **Specification** | `OrderSpecifications` | Composable filters (status, customer) instead of one repository method per combination. |
| **DTO + Mapper/Adapter** | `CreateOrderRequest`, `OrderResponse`, `PageResponse`, `OrderMapper` | API contract is independent of the JPA entity. |
| **CQRS (light)** | Commands for writes, `OrderQueryService` for reads | Read path uses read-only transactions and batch fetching. |
| **Adapter** | `DatabaseUrlEnvironmentPostProcessor` | Converts a platform `DATABASE_URL` into JDBC settings. |
| **Messaging (EIP)** | `JmsOrderEventPublisher`, `OrderStatusUpdateListener`, `MessagingConfig` | Event message channel, competing consumers, dead-letter channel, message expiry. |
| **Proxy** | Spring AOP behind `@Transactional` handlers | Transactions without boilerplate. |

## Key behaviour

**Background job**: `PendingOrderProcessor` runs every 5 minutes (`orders.processing.interval`), dispatching
`PromotePendingOrdersCommand`. The handler runs one set-based SQL statement,
`UPDATE orders SET status='PROCESSING', version=version+1 … WHERE status='PENDING'`, backed by the
`(status, created_at)` index. It is idempotent and safe across several instances.

**Cancel vs. job race**: cancel loads the order, `PendingState` allows it, and the flush checks `@Version`.
If the job promoted the order in between, the version no longer matches and the client gets `409`.
An order can never end up both processed and cancelled.

**Efficiency**: order + items in one entity-graph query; lists use batch fetching (no N+1); totals are
computed server-side with `BigDecimal`.

## Tests

Integration suites are written **once** as abstract `*Contract` classes and run on **both** databases:

| Tier | Naming | Command | Database | Docker? |
|---|---|---|---|---|
| Unit | `*Test` | `mvn test` | none | no |
| Integration on H2 | `*H2Test` | `mvn test` | in-memory H2 (MySQL mode) + embedded Artemis | no |
| Integration on PostgreSQL | `*PostgresIT` | `mvn verify` (Failsafe) | PostgreSQL 16 + Artemis 2.57.0, both via Testcontainers | yes |

Each concrete class picks its database and broker in one `@DynamicPropertySource` method via `support/TestDatabases` and `support/TestBrokers`:

```java
class OrderApiH2Test extends OrderApiContract {
    @DynamicPropertySource static void infrastructure(DynamicPropertyRegistry r) { TestDatabases.h2(r); TestBrokers.embedded(r); }
}
class OrderApiPostgresIT extends OrderApiContract {
    @DynamicPropertySource static void infrastructure(DynamicPropertyRegistry r) { TestDatabases.postgres(r); TestBrokers.artemis(r); }
}
```

The PostgreSQL and Artemis containers are lazily started singletons shared by all `*PostgresIT` classes, so H2-only runs
never touch Docker. H2 gives fast feedback anywhere; PostgreSQL proves the production schema and SQL.
Docker Desktop, Colima or Rancher Desktop all work; GitHub Actions has Docker built in and runs `mvn verify`.

| Suite | H2 | PostgreSQL | Covers |
|---|---|---|---|
| `OrderRepositoryContract` | `OrderRepositoryH2Test` | `OrderRepositoryPostgresIT` (+ partial index, server version) | Flyway migration, mapping, Specifications, bulk update, optimistic lock, constraints, cascade delete |
| `OrderApiContract` | `OrderApiH2Test` | `OrderApiPostgresIT` | Every endpoint end-to-end (MockMvc), validation, filters, paging, lifecycle, cancel rules, events |
| `PendingOrderProcessorContract` | `PendingOrderProcessorH2Test` | `PendingOrderProcessorPostgresIT` | Job promotes only PENDING, idempotent, one event per run, blocks later cancel, stale cancel rejected |
| `SchedulerRunsAutomaticallyContract` | `SchedulerRunsAutomaticallyH2Test` | `SchedulerRunsAutomaticallyPostgresIT` | Spring's scheduler really fires the job |
| `OrderMessagingContract` | `OrderMessagingH2Test` | `OrderMessagingPostgresIT` | Events published after commit (JSON + JMS properties), inbound updates applied, bad messages dropped without blocking, State rules enforced |

Unit tests: `OrderStateTest`, `PricingStrategyTest`, `OrderValidationChainTest`, `CommandBusTest`,
`AbstractScheduledJobTest`, `DatabaseUrlEnvironmentPostProcessorTest`.

## Configuration

```yaml
orders:
  processing: { interval: PT5M, initial-delay: PT1M, enabled: true }
  messaging:  { enabled: true, events-queue: orders.events, status-updates-queue: orders.status-updates }
  pricing:    { strategy: standard }        # or bulk-discount
  validation: { max-order-value: 1000000 }
```

## Production notes

- Look prices up from a catalog service rather than trusting the request; add an `Idempotency-Key` header.
- Add authentication so customers only see and cancel their own orders.
- For guaranteed event delivery to other systems, use a transactional outbox instead of in-process events.
