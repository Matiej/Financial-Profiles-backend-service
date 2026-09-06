# Changelog

All notable changes to this project are recorded in this file.

Format loosely based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/). The project
**doesn't have CI/CD or automated versioning yet** (see
`info/jira/2026-003_ci_cd_pipeline_and_versioning.md`) — the version in `pom.xml` is bumped by
hand. This file starts at version `0.0.2` (the current one at the time this file was created,
2026-08-30); earlier history (since `0.0.1`) can only be traced through `git log` and the daily
reports under `info/done/*_report.md` — it is not reconstructed retroactively here.

## [Unreleased]

## [0.0.3] — 2026-09-06

Closes jira `2026-005` (n8n v2 payload — test coverage for the shape) and fixes a build bug that
left unit tests silently unrun.

### Fixed
- **`mvn test` was running 0 unit tests.** Surefire used its default `<includes>` pattern
  (`*Test`, `*Tests`), which doesn't match Spock's naming convention (`*Spec`) — the build reported
  `BUILD SUCCESS` without executing a single test. Fixed by splitting the Groovy test compilation
  into two output directories: `gmavenplus-plugin` now compiles `src/test/groovy` (unit) into
  `target/test-classes`, and `src/integration/groovy` (integration, Testcontainers) into a separate
  `target/integration-test-classes`; surefire got an explicit `<include>**/*Spec.groovy</include>`
  and scans only the first directory, failsafe (`<testClassesDirectory>`) only the second. Result:
  `mvn test` now actually runs **67 unit tests** (green, ~2–3s, no Docker needed).
- **Follow-up bug from the fix above:** pointing failsafe's `testClassesDirectory` at the new
  `target/integration-test-classes` silently dropped the default `target/test-classes` off its
  classpath — including the test resources copied there by the standard `testResources` goal
  (`application-test.yml`, `logback-test.xml`, ...), which is untouched and still writes only to
  the default directory. Every integration spec failed at Spring context startup with
  `Could not resolve placeholder 'spring.ai.openai.api-key'` (the dummy key lives in
  `application-test.yml`). Fixed by adding `target/test-classes` back via failsafe's
  `<additionalClasspathElements>`. Verified end-to-end with `./mvnw clean verify` against a live
  Docker daemon: **292 tests total (67 unit + 225 integration), 0 failures, `BUILD SUCCESS`.**

### Added
- **Test asserting the n8n v2 webhook payload shape** (`ClientTestControllerSpec`) — closes jira
  `2026-005`. Parses the real body captured by WireMock and verifies the envelope (`clientEmail`,
  `testSubmissionPublicId`, `submissionId`, `testId`), `overallSummary`, `profiles[].answersBySeverity[]`,
  and the absence of the old v1 field `clientTestAnswerNotificationList`. Previously only the fact
  that n8n was called was tested, not the shape of what was sent.

## [0.0.2] — 2026-07-05

First version with the **editable Profiles + full StatementDefinition CRUD** feature fully
implemented (EMAT-055, plan: `info/done/statement_definiotion_user_edit.md`, phases F1–F9). The
previous version (`0.0.1`) carried a hardcoded `StatementProfile` enum (8 baked-in profiles) — this
version replaces it with a fully dynamic, editable model.

### Added
- **Editable `Profile`** (Mongo document, `profiles` collection) — full CRUD under
  `/api/definition/profile` (`ProfileController`): `plName`/`blockingName`/`transitionalName`/
  `resourcesName`/`order`, soft-delete + guard **409 `PROFILE_IN_USE`** when an active definition
  still references the profile. Seeded on startup with 8 profiles (`profil_1`..`profil_8`,
  migration `v008`).
- **Full `StatementDefinition` CRUD** under `/api/definition` — `PUT`/`DELETE` alongside the
  existing `POST`/`GET`; `id` (Mongo) replacing the old `statementId`; `profileId` replacing the
  `category` enum; `statementKey` immutable, server-generated (`sk_<UUID>`) for new definitions;
  guards **409 `DEFINITION_IN_USE`** (DELETE of a definition used in an active FpTest) and
  **409 `DEFINITION_EDIT_ERROR`** (PUT changing `profileId` on a definition that already has
  submissions).
- **`FpTest` DELETE → soft-delete** (202 instead of physical removal); 409
  `FP_TEST_DELETE_ERROR` only for a live token (submission `OPEN`, not expired); `FpTestResponse`
  enriched with `submissionsDone`/`submissionsOpenActive`/`submissionsOpenExpired` counters (for
  the FE confirmation dialog).
- **`SubmissionResponse.testName`** — test name resolved server-side from the FpTest (also for
  soft-deleted tests), so the FE no longer stitches it together from a separate call.
- **"Token resurrection" guard** — `POST`/`PUT /api/submission` now validate that `testId` points
  to an existing, non-deleted FpTest (404 `TEST_NOT_FOUND` / 409 `TEST_DELETED`); previously `POST`
  didn't validate `testId` at all.
- **Soft-delete of a completed client test (scoring)** — `DELETE /api/profiler/{testSubmissionPublicId}`
  (204, idempotent, 404 `CLIENT_TEST_SUBMISSION_ERROR`); backfilled by migration `v009`.
- **Profile snapshot (`profileSnapshots`) in `ClientTestDocument`** — profile labels are frozen at
  the moment a client test is completed; scoring and the AI report read exclusively from the frozen
  data, so a later edit/deletion of the live profile doesn't change historical results
  ("live vs snapshot").
- **Global label thresholds** (`ScoringLabelResolver`, config `app.scoring.thresholds`) — blocking
  ≤ 0%, resources ≥ 68%, independent of the profile.
- **`FpTestStatementDto.profileId`** — profile identity (alongside the name) in `/api/pftest*`
  responses, for FE-side grouping/sorting/linking without joining on `statementKey`.
- **n8n webhook v2** (`POST /score-test/email`) — payload rebuilt from raw answers
  (`clientTestAnswerNotificationList`) to already-computed scoring (`overallSummary` + `profiles[]`,
  the same shape as `GET /api/profiler/{tspi}/scoring`), enriched with `clientEmail` and dates
  (`submissionDate`, `clientTestDate`).

### Changed
- **BREAKING:** `StatementDefinitionResponse` — `category` (enum `PROFIL_1`..`PROFIL_8`) →
  `profileId` (string slug); `statementId` → `id`.
- **BREAKING:** definition filter: `GET /api/definition/category?category=PROFIL_1` →
  `GET /api/definition?profileId=profil_1`.
- **BREAKING:** `FpTestStatementDto.statementsCategory` → `statementsProfile` (rename only, value
  unchanged: live profile name with a fallback to the slug).

### Removed
- `StatementProfile` enum (8 hardcoded profiles + labels) — fully replaced by the dynamic `Profile`
  model.
- Legacy **Tally** feature (and its Mongo collection, migration `v007`) — unused, removed entirely
  (EMAT-054).
- Old n8n v1 payload (`ClientAnswerNotification`, raw answers) — replaced by v2.

### Known issues (inherited, still open)
- Admin can't delete a submission in `DONE` status (only hard-delete for `OPEN`) — jira `2026-007`,
  deliberately deferred.
- No retry/outbox for failed n8n notifications (an error/timeout is silently swallowed today) —
  jira `2026-006`, still under discussion.
