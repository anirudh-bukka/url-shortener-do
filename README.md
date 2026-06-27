# URL Shortener REST API Service

A production-ready REST API (Java 21 + Spring Boot 3) that accepts a long URL,
generates a short alias, redirects visitors to the original destination, and
**handles concurrent creation requests safely**. Designed to deploy on
DigitalOcean App Platform (or a Droplet) with a managed PostgreSQL database.

---

## Features

- `POST /api/v1/urls` — create a short URL (auto-generated or custom alias)
- `GET /api/v1/urls/{alias}` — inspect an alias (full metadata: id, destination, created/last-accessed time, hit count)
- `GET /{alias}` — 302 redirect to the original URL (cache-accelerated; 302 keeps access counts accurate)
- Concurrency-safe alias generation (DB sequence + Base62) — **no collisions**
- Custom-alias races resolved by a UNIQUE constraint → clean `409 Conflict`
- Bean-validated input, consistent JSON error envelope
- Flyway-managed schema, Actuator health/metrics, OpenAPI/Swagger UI
- Dockerised, with CI and a DigitalOcean App Platform spec

---

## How concurrency safety works

| Scenario | Mechanism | Result |
| --- | --- | --- |
| Many parallel auto-generate requests | Each draws a unique value from the Postgres `url_mapping_seq` sequence, then Base62-encodes it | Aliases are unique *by construction* — no locks |
| Two requests race for the same **custom** alias | `UNIQUE` index on `short_code`; the losing `INSERT` throws `DataIntegrityViolationException` | Exactly one wins; others get `409 Conflict` |
| A generated code happens to equal an existing **custom** alias | Insert retried with the next sequence value, each attempt in its own `REQUIRES_NEW` transaction (`UrlMappingWriter`) | Caller transparently gets a unique alias |
| Concurrent redirects to one alias | Atomic `UPDATE ... SET hit_count = hit_count + 1` in the database | Access counts stay correct without read-modify-write races |
| Hot redirect traffic | `@Cacheable` (Redis if configured, else in-process) | Low-latency reads, fewer DB hits |

Both creation guarantees are verified by `UrlShortenerServiceConcurrencyTest`
against a real PostgreSQL container (50-thread auto-generate race + 20-thread
custom-alias race).

---

## Tech stack

Java 21 · Spring Boot 3.3 · Spring Web / Data JPA · PostgreSQL · Flyway ·
Redis (optional) · springdoc-openapi · Testcontainers · Maven · Docker.

---

## Project structure

Every folder and file, with its purpose:

```
url-shortener/
├── pom.xml                      # Maven build: dependencies, Java 21, plugins, image build goal
├── mvnw, mvnw.cmd              # Maven Wrapper — build with no global Maven install (./mvnw ...)
├── .mvn/wrapper/
│   └── maven-wrapper.properties # Pins the exact Maven version the wrapper downloads
├── .gitignore                  # Excludes build output, IDE files, secrets (.env)
├── .dockerignore               # Keeps the Docker build context small/reproducible
├── .env.example                # Template env vars for local docker-compose (copy to .env)
├── Dockerfile                  # Multi-stage build → slim JRE runtime image (non-root, healthcheck)
├── docker-compose.yml          # Local stack: app + PostgreSQL + Redis, one command
├── README.md                   # This document
│
├── .do/
│   └── app.yaml                # DigitalOcean App Platform spec (service + managed Postgres + CD)
│
├── .github/workflows/
│   └── ci.yml                  # GitHub Actions: build + test on every push/PR
│
├── src/main/java/com/example/urlshortener/
│   ├── UrlShortenerApplication.java     # Spring Boot entrypoint; enables caching & typed config
│   │
│   ├── config/                          # Cross-cutting configuration
│   │   ├── AppProperties.java           #   Validated app.* settings (base URL, reserved paths)
│   │   ├── CacheConfig.java             #   Cache wiring; in-memory fallback when Redis is absent
│   │   └── OpenApiConfig.java           #   Swagger/OpenAPI document metadata
│   │
│   ├── controller/                      # HTTP layer (thin; delegates to services)
│   │   ├── UrlController.java           #   /api/v1/urls — create & inspect aliases
│   │   └── RedirectController.java      #   /{alias}   — 301 redirect to the long URL
│   │
│   ├── service/
│   │   ├── UrlShortenerService.java     # Core business logic + concurrency-safe alias creation
│   │   └── UrlMappingWriter.java        # Isolated (REQUIRES_NEW) insert so collisions can be retried cleanly
│   │
│   ├── repository/
│   │   └── UrlMappingRepository.java    # Spring Data JPA access + sequence/next-val query
│   │
│   ├── domain/
│   │   └── UrlMapping.java              # JPA entity mapping alias ⇄ long URL (the table model)
│   │
│   ├── dto/                             # API request/response contracts (decoupled from entities)
│   │   ├── CreateShortUrlRequest.java   #   Validated POST body (url, optional customAlias)
│   │   ├── ShortUrlResponse.java        #   JSON returned for create/inspect
│   │   └── ErrorResponse.java           #   Consistent error envelope for all 4xx/5xx
│   │
│   ├── exception/                       # Error types + central translation to HTTP
│   │   ├── GlobalExceptionHandler.java  #   @RestControllerAdvice → maps exceptions to status codes
│   │   ├── AliasAlreadyExistsException.java  # 409 Conflict (custom alias taken)
│   │   ├── ReservedAliasException.java       # 400 Bad Request (alias collides with a route)
│   │   └── ResourceNotFoundException.java    # 404 Not Found (unknown alias)
│   │
│   └── util/
│       └── Base62.java                  # Stateless Base62 encode/decode codec
│
├── src/main/resources/
│   ├── application.yml                  # Base config (datasource, JPA, Flyway, actuator, app.*)
│   ├── application-dev.yml              # Local dev overrides (verbose logging, SQL echo)
│   ├── application-prod.yml             # Production overrides (lean logging, larger pool)
│   └── db/migration/
│       └── V1__create_url_mapping.sql   # Flyway baseline: sequence, table, unique index
│
└── src/test/
    ├── java/com/example/urlshortener/
    │   ├── AbstractIntegrationTest.java            # Testcontainers Postgres base class
    │   ├── UrlShortenerApplicationTests.java       # Context-loads / migration smoke test
    │   ├── util/Base62Test.java                    # Pure unit tests for the codec
    │   ├── service/UrlShortenerServiceConcurrencyTest.java  # Proves concurrent-creation safety
    │   ├── service/UrlShortenerServiceTest.java    # Creation, resolution, metadata, validation
    │   └── controller/UrlControllerTest.java       # Web-slice tests (incl. 302 redirect + counting)
    └── resources/
        └── application-test.yml                    # Test profile (non-DB settings)
```

---

## Running locally

```bash
./mvnw -Plocal spring-boot:run
```

This is ideal inside containers/dev environments where you can't run Docker. Data is
kept only for the lifetime of the process; browse it at http://localhost:8080/h2-console
(JDBC URL `jdbc:h2:mem:urlshortener`, user `sa`, empty password). H2 is scoped to this
profile only, so the default build and the production Docker image stay PostgreSQL-only.

Then open:
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

---

## API usage

Create a short URL (auto alias):

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://www.digitalocean.com/products/app-platform"}'
```

Create with a custom alias:

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/launch","customAlias":"launch"}'
```

Follow the redirect:

```bash
curl -i http://localhost:8080/launch        # → 302 Location: https://example.com/launch
```

Inspect metadata:

```bash
curl http://localhost:8080/api/v1/urls/launch
```

```json
{
  "id": 1000001,
  "shortCode": "launch",
  "shortUrl": "http://localhost:8080/launch",
  "longUrl": "https://example.com/launch",
  "createdAt": "2026-06-27T09:00:00Z",
  "lastAccessedAt": "2026-06-27T09:05:12Z",
  "hitCount": 42
}
```

---

## Testing

```bash
./mvnw test            # unit + web-slice tests
./mvnw verify          # everything, incl. Testcontainers integration tests (needs Docker)
```

---

## Deploying to DigitalOcean

1. Push this repo to GitHub and edit `repo:` in `.do/app.yaml`.
2. Deploy: `doctl apps create --spec .do/app.yaml` (or via the App Platform UI).
3. App Platform builds the `Dockerfile`, provisions managed PostgreSQL, injects
   the connection string, runs Flyway on startup, and health-checks
   `/actuator/health/readiness`. `instance_count: 2` runs it scaled out — the
   sequence-based design stays correct across instances.

For a Droplet instead, build and run the image directly:

```bash
docker build -t url-shortener .
docker run -p 8080:8080 --env-file .env url-shortener
```

---

## Configuration reference

| Env var | Purpose | Default |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `dev` or `prod` | `dev` |
| `APP_BASE_URL` | Origin used to build short links | `http://localhost:8080` |
| `SPRING_DATASOURCE_URL` | JDBC URL for Postgres | local Postgres |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | DB credentials | `urlshortener` |
| `SPRING_DATA_REDIS_HOST` | Redis host (blank = in-memory cache) | _(blank)_ |
| `PORT` | HTTP port | `8080` |
```