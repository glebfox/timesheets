# Testing

Project-specific mechanics of the test harness. How to write Jmix tests in general is the `jmix-create-test` skill; the command is in `ARCHITECTURE.md`.

## Harness

- The `test` profile runs on in-memory HSQLDB. Liquibase applies the schema and the `admin` user only (context `prod`), with no demo data, so each test creates the data it needs.
- The in-memory database is shared by every test in a run. Tests remove what they created in `@AfterEach` with soft deletion turned off, as the existing tests do, so no rows are left for the next test.
- Non-UI tests authenticate through the `test_support/AuthenticatedAsAdmin` extension.
- UI tests boot with `TabbedModeTestAssistConfiguration` in place of the skill's `FlowuiTestAssistConfiguration`, because the main view is a tabbed-mode main view. See `UserUiTest`.
- The `test` profile sets `jmix.ui.component.dialogs-opened-change-events-enabled`. Without it, option and message dialogs publish no opened event, and `UiTestUtils.getLastOpenedDialog()` returns null even while a dialog is open. See `CalendarUiTest`.
- HSQLDB has none of the unique indexes (`docs/guidelines/database.md`), so no test can check name uniqueness.
- The `clean test` gate of the `jmix-verify-bootrun` skill runs through local CI (`ARCHITECTURE.md`), never as `./gradlew clean` in this checkout: `bootRun` and the IDE run the app from this checkout's `build/`, and `clean` deletes the classes under it. A plain `SPRING_PROFILES_ACTIVE=test ./gradlew test` here is fine.

## Local overrides in `.jmix/conf`

Jmix reads a resource from `jmix.core.conf-dir` (`.jmix/conf/` in the project directory by default) before the classpath, both at runtime and in tests. Jmix Studio's hot deploy writes changed resources there, and the directory is gitignored, so after a branch switch a stale copy can win over the checked-out file. Typical symptom: `NoSuchViewException: View '<id>' is not defined` while the menu is built, from a stale `menu.xml` that names a view the current branch does not have. Fix: delete the stale file under `.jmix/conf/`. CI never has this directory.

## Live checks

Start the application with the `dev` profile for demo data (users in `liquibase/changelog/init-data/010-init-user.xml`); the render walk is described in the `jmix-verify-bootrun` skill. Only a live check can verify:

- server-push updates, such as the rejected-entries badge refreshing in another user's open session;
- how screens look: badge colors, icons, card layouts.

A running app picks up Java changes only after a restart; descriptor and CSS changes reach it after `./gradlew processResources`, then a logout and login, because in tabbed mode a plain reload keeps the old view instance. The `live-checker` agent runs a live check against the app the developer has running.
