---
name: local-ci
description: Use when a change in this project needs its closing check — the full test suite, the `clean test` gate of jmix-verify-bootrun, or the CI checks (license headers and tests) before opening or updating a pull request.
---

# Local CI

The developer runs the app from this checkout's `build/`, so `./gradlew clean` here deletes the classes under a running app. Run the CI checks in a throwaway clone instead:

```bash
bash .skills/local-ci/scripts/local-ci.sh
```

The script takes HEAD plus uncommitted and untracked changes and runs the steps of `.github/workflows/tests.yml`: `spotlessCheck`, then the tests on the `test` profile. It takes minutes, so run it in the background. This run is the `clean test` gate; never run `clean` in this checkout.

A quick loop over one test class can stay in this checkout, since it does not clean: `SPRING_PROFILES_ACTIVE=test ./gradlew test --tests '<Class>'`.

## Reading the result

- **PASSED** — the clone is deleted.
- **FAILED** — the clone is kept; the script prints the failing tasks, the failing test classes, the report, and the log.
- A `spotlessCheck` failure is a missing license header: run `./gradlew spotlessApply` in this checkout (it does not touch `build/`).
- The clone has no `.jmix/conf`. A test that passes here but fails in this checkout points at a stale local override (`docs/guidelines/testing.md`).

It proves what CI proves. How screens render needs a live check: the `live-checker` agent.
