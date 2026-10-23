# Architecture

A single-module Jmix application (Spring Boot, Vaadin Flow UI) for logging time spent on project tasks. Versions and add-ons are in `build.gradle`.

## Where things live

Java code is under `com.glebfox.timesheets` in `src/main/java`; view descriptors, message bundles, and changelogs are under the same package in `src/main/resources`.

- `entity/<area>/` — entities and their enums, one subpackage per domain area, and `misc` for embeddables. Specs: `docs/specs/data-model/`, starting with its `README.md`.
- `datatype/` — custom value types with their Jmix datatypes and JPA converters.
- `app/` — beans with the domain queries and helpers that views call (`*Support`); `app/util/` holds the value types they return and the builders of UI pieces that several views share.
- `view/<entity>/` — view controllers; each descriptor sits at the matching path under resources. Specs: `docs/specs/views/`.
- `action/` — custom list actions.
- `data/renderer/` — reusable grid renderers.
- `component/` — custom UI components and icon sets. A component usable in descriptors is declared in `app-ui-components.xsd`, registered in `TimesheetsApplication`, and described for Jmix Studio in the package's `@StudioUiKit` holder, with its palette icons under `meta/icon/` in resources; the icon sets' JavaScript is in `src/main/frontend/src/component/icon/`.
- `listener/`, `event/` — entity event listeners and the UI events they publish to open sessions.
- `security/` — resource roles, the database user repository, and the security configuration. Specs: `docs/specs/roles/`.
- `router/error/` — custom error pages; their HTML templates are in `META-INF/resources/errorpages/`.
- `liquibase/changelog/` — schema changelogs by date (`YYYY/MM/`) and demo data in `init-data/`. See `docs/guidelines/database.md`.
- `menu.xml`, `messages_*.properties` — the main menu and the message bundles, one per locale.
- `META-INF/resources/themes/timesheets-aura/` — the application theme, built on Aura.

Agent tooling sits outside `src/`. Skills are in `.skills/`, one directory each; `.claude/skills/` and `.agents/skills/` link to them by symlinks, so Claude Code and other agents read the same files. Claude Code subagents are in `.claude/agents/`, and its hooks are in `.claude/hooks/`, wired in `.claude/settings.json`. The hooks need `jq` and `python3` on the PATH, and the skills that touch GitHub need `gh` signed in with the `project` scope.

## Profiles

- Default (`application.properties`): PostgreSQL database `ts` on `localhost`; Liquibase context `prod`, which creates the schema and the `admin` user only.
- `dev`: Liquibase context `dev`, which also loads the demo data; the login form is pre-filled with `admin`/`admin`.
- `test`: in-memory HSQLDB.

## Commands

- Run with demo data: `SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`. It needs the local PostgreSQL database.
- Tests: `SPRING_PROFILES_ACTIVE=test ./gradlew test`. Without the `test` profile the tests try to reach PostgreSQL. CI runs the same in `.github/workflows/tests.yml`.
- Local CI: `bash .skills/local-ci/scripts/local-ci.sh` runs `spotlessCheck` and the tests on the `test` profile, as CI does, in a throwaway clone of HEAD plus uncommitted and untracked changes, so this checkout's `build/` is left alone. On failure it keeps the clone and prints the failing tasks and test classes.
- License headers: `./gradlew spotlessApply` adds the Apache License header to the files under `src/` and the Gradle scripts, and CI fails on a missing one with `spotlessCheck`. In Java, XML, HTML, and properties files the header is everything above the `package` line, the first element, or the first key, so a comment placed there is replaced. JavaScript is not covered: the only scripts are third-party icon sets, listed in `NOTICE` with the other third-party assets.
- Production jar: `./gradlew bootJar -Pvaadin.productionMode=true`.
- Dependency resolution needs Jmix premium repository credentials: Gradle properties `premiumRepoUser`/`premiumRepoPass`, or environment variables `PREMIUM_REPO_USER`/`PREMIUM_REPO_PASS`.
