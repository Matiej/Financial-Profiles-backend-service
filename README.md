# reapi — profiler-service

Backend for a financial/psychological **profiling test** product. An admin builds tests
(`FpTest`) out of statement definitions; a client takes a test via a public token; answers are
scored per **profile** and an AI insight report is generated. Notifications go out via n8n webhooks.

**Stack:** Java 21 · Spring Boot 3.5 (reactive — WebFlux + reactive MongoDB, Netty) ·
Spring Security (OAuth2 resource server / JWT) · Mongock migrations · Lombok · Maven.

Runs on port **8100**.

## Two API consumers

- **Admin panel** — builds/edits tests, statement definitions, profiles; views scoring/insight
  reports. Uses the authenticated `/api/...` endpoints (roles `BUSINESS_ADMIN` / `TECH_ADMIN`).
- **Client test app** (public) — the page a client opens to take the test. Talks only to the public,
  token-based `ClientTestController` at `/api/client/test` (`GET /{publicToken}` to fetch questions,
  `POST` to submit answers). No auth — secured by the public token + submission state.

## Requirements

- JDK 21
- Docker (only for integration tests — Testcontainers spins up `mongo:7.0`)
- No global Maven needed; use the bundled wrapper `./mvnw`

## Run

```bash
./mvnw spring-boot:run     # start the app on http://localhost:8100
```

## Test

```bash
./mvnw test          # unit tests only (src/test/groovy, Spock) — fast; excludes integration
./mvnw verify        # full: unit + integration tests (Testcontainers) + build
./mvnw -q test-compile   # compile main + all tests without running
```

- **Unit tests:** `src/test/groovy` — Spock, pure-logic specs.
- **Integration tests:** `src/integration/groovy` — Spock + Testcontainers (`mongo:7.0`), real
  WebFlux via `WebTestClient`. A **separate Maven source set** (run by `verify`, not by `mvn test`).

## Documentation

- **`CLAUDE.md`** — architecture, domain rules, and conventions (also the guide for AI agents).
- **`info/`** — working docs: the feature plan (`statement_definiotion_user_edit.md`), the FE-facing
  contract doc (`statement_definiotion_user_edit_fe_dev_info.md`), and dated `*_report.md` logs.
- **`info/jtra/`** — task tickets.

> Full project documentation is being moved to a MkDocs site (GitHub Pages) —
> see `info/jtra/2026-001_add_mkdocs_for_app.md`.
