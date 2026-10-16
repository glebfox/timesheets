# Architecture

A single-module Jmix application (Spring Boot, Vaadin Flow UI) for logging time spent on project tasks. Versions and add-ons are in `build.gradle`.

## Where things live

Java code is under `com.glebfox.timesheets` in `src/main/java`; view descriptors, message bundles, and changelogs are under the same package in `src/main/resources`.

- `entity/<area>/` — JPA entities and their enums: `client`, `project` (projects, participants, project roles), `task` (tasks, task types), `tag` (tag groups, tags), `timeentry`, `user`, and `misc` for embeddables. Specs: `docs/specs/data-model/`, starting with its `README.md`.
- `datatype/` — the `SpentTime` value type, its Jmix datatype, and its JPA converter.
- `app/` — beans with the domain queries and helpers that views call (`*Support`, `TimeParser`). `app/util/ComponentSupport` builds shared UI pieces: avatars, the user cell, the time-entry status badge, project-role badge colors, and the task-with-project cell. Not every view uses it yet: the tags badges, the project and task status badges, and the main view's avatar are still built in their views.
- `view/<entity>/` — view controllers; each descriptor sits at the matching path under resources. Specs: `docs/specs/views/`.
- `action/` — custom list actions (`list_timeEntry_copy`).
- `data/renderer/` — reusable grid renderers (badge, user).
- `component/` — custom UI components and icon sets. `imageUpload` is usable in descriptors through the `app-ui-components.xsd` namespace and is registered in `TimesheetsApplication`; the icon sets' JavaScript is in `src/main/frontend/src/component/icon/`. `component/themeswitcher` is not used: the user menu uses the framework's theme-switch action.
- `listener/`, `event/` — the time-entry listener and the UI event it publishes to open sessions.
- `security/` — resource roles, the database user repository, and the security configuration. Specs: `docs/specs/roles/`.
- `router/error/` — custom "not found" and "access denied" pages; their HTML templates are in `META-INF/resources/errorpages/`.
- `liquibase/changelog/` — schema changelogs by date (`YYYY/MM/`) and demo data in `init-data/`. See `docs/guidelines/database.md`.
- `menu.xml`, `messages_en.properties`, `messages_ru_RU.properties` — the main menu and the two message bundles.
- `META-INF/resources/themes/timesheets-aura/` — the application theme, built on Aura.

## Profiles

- Default (`application.properties`): PostgreSQL database `ts` on `localhost`; Liquibase context `prod`, which creates the schema and the `admin` user only.
- `dev`: Liquibase context `dev`, which also loads the demo data; the login form is pre-filled with `admin`/`admin`.
- `test`: in-memory HSQLDB.

## Commands

- Run with demo data: `SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`. It needs the local PostgreSQL database.
- Tests: `SPRING_PROFILES_ACTIVE=test ./gradlew test`. Without the `test` profile the tests try to reach PostgreSQL. CI runs the same in `.github/workflows/tests.yml`.
- License headers: `./gradlew spotlessApply` adds the Apache License header to the files under `src/` and the Gradle scripts, and CI fails on a missing one with `spotlessCheck`. In Java, XML, HTML, and properties files the header is everything above the `package` line, the first element, or the first key, so a comment placed there is replaced. JavaScript is not covered: the only scripts are third-party icon sets, listed in `NOTICE` with the other third-party assets.
- Production jar: `./gradlew bootJar -Pvaadin.productionMode=true`.
- Dependency resolution needs Jmix premium repository credentials: Gradle properties `premiumRepoUser`/`premiumRepoPass`, or environment variables `PREMIUM_REPO_USER`/`PREMIUM_REPO_PASS`.
