# Testing

Project-specific mechanics of the test harness and of the IDE's static checks. How to write Jmix tests in general is the `jmix-create-test` skill; the command is in `ARCHITECTURE.md`.

## Harness

- The `test` profile runs on in-memory HSQLDB. Liquibase applies the schema, the `admin` user, and the default work schedule "Full time", with no demo data, so each test creates the rest of the data it needs.
- The in-memory database is shared by every test in a run. Tests remove what they created in `@AfterEach` with soft deletion turned off, so no rows are left for the next test; a row that a listener or a soft delete changed after the test saved it is reloaded by id, with soft deletion off, right before removal. A test that may change or soft-delete a seeded row, such as the default work schedule, puts it back the same way, so one failure does not cascade through the run. See `DefaultWorkScheduleTest`.
- `DemoDataLoaderTest` starts the application with the `test` and `dev` profiles on an in-memory database of its own, so the demo data never reaches the database the other tests share.
- Non-UI tests authenticate through the `test_support/AuthenticatedAsAdmin` extension.
- UI tests boot with `TabbedModeTestAssistConfiguration` in place of the skill's `FlowuiTestAssistConfiguration`, because the main view is a tabbed-mode main view. See `UserUiTest`.
- The `test` profile sets `jmix.ui.component.dialogs-opened-change-events-enabled` and `jmix.ui.component.notifications-opened-change-events-enabled`. Without them, option and message dialogs and notifications publish no opened event, so `UiTestUtils.getLastOpenedDialog()` and `UiTestUtils.getLastOpenedNotification()` return null even while one is shown. See `CalendarUiTest` and `AvatarUploadUiTest`.
- `UiTestUtils.getLastOpenedDialog()` returns only component dialogs, such as option and message dialogs. A view opened as a dialog, such as a detail view in dialog mode or a view opened with `DialogWindows`, is reached with `UiTestUtils.getLastOpenedViewDialog()`, or as the topmost view with `UiTestUtils.getCurrentView()`. See `WorkScheduleDeletionUiTest`.
- An action's `actionPerform(...)` runs it whether it is enabled or not. A UI test asserts `isEnabled()` before it performs an action, or it passes for an action the user could never click.
- An exception thrown by an after-commit listener, one annotated `@TransactionalEventListener`, does not reach the code that saved: Spring logs it as `TransactionSynchronization.afterCompletion threw exception` and the save returns normally, so a test passes while the listener fails. A test of such a listener checks what the listener does, such as the UI events it publishes, seen through a `@MockitoSpyBean` of `UiEventPublisher`; where the listener must do nothing, the test also asserts that the output captured by `OutputCaptureExtension` has no such line. See `TimeEntryEventListenerTest`.
- Data compared with today, such as which assignment is in force, is dated relative to `LocalDate.now()`. A fixed date that is still in the future when the test is written is in the past some day later, and the test starts failing then.
- A UI test shows views in another locale by setting `ClientDetails` with that locale as the details of the authentication inside `systemAuthenticator.runWithUser(...)`: views opened in that block take their texts and formats from it, and the change ends with the block. See `WorkScheduleEditorUiTest` and `HolidayCalendarDaysAddUiTest`.
- Cancel on a detail view with unsaved changes opens the save confirmation dialog, which `jmix.ui.view.use-save-confirmation` turns on by default; a test discards the changes by clicking its button with the id `close` (`ok` saves). `closeWithDiscard()` skips the dialog a user sees. See `UserAssignmentsUiTest`.
- A test of a resource role gives a test user the role by saving a `RoleAssignmentEntity` row of type `RESOURCE` and runs as that user with `systemAuthenticator.runWithUser(...)`; role assignments are soft-deleted, so the teardown removes the row with soft deletion off. Opening a view checks the current user's view policies, so a test proves a role's view policies only by opening the views inside that block. See `WorkScheduleManagementRoleUiTest`.
- A view that exists only for tests, such as one that loads a component with every XML attribute set, lives in `src/test` under the base package, with its descriptor in `src/test/resources`: Jmix finds views on the test classpath too. See `AvatarUploadTestView`.
- A UI test sees what the server asks the browser to do, such as a grid's `recalculateColumnWidths()`, only as a JavaScript call queued for the browser: it runs the UI's executions before the client response and reads the pending invocations of the component's node. See `browserCalls` in `WorkScheduleListUiTest`.
- Nothing requests what a component serves through a `DownloadHandler`, such as an avatar's image, in a UI test. A test of what the browser would get calls Vaadin's `StreamRequestHandler` itself, with a mocked `VaadinRequest` whose path info is `/` followed by the URL in the component's attribute, such as an avatar's `img`, and a mocked `VaadinResponse` whose output stream collects the body. The response's `getService()` must return the session's service: a download without a content type takes it from there and fails without it. A refusal, such as the 403 for a component that is disabled, hidden, or not attached, shows only as a `sendError` call, so the test verifies that none was made. See `ComponentSupportUiTest`.
- A value set on a filter condition from the server, such as the name condition of a `propertyFilter` or of a `genericFilter` configuration, does not reload the list by itself; the test then calls the filter's `apply()`. See `WorkScheduleListUiTest`.
- HSQLDB gets none of the partial unique indexes of soft-deleted entities, which are created for PostgreSQL only (`docs/guidelines/database.md`), so no test can check uniqueness among non-deleted rows, such as a unique name, a holiday calendar's dates, or one assignment per user and start date.
- The `clean test` gate of the `jmix-verify-bootrun` skill runs through local CI (`ARCHITECTURE.md`), never as `./gradlew clean` in this checkout: `bootRun` and the IDE run the app from this checkout's `build/`, and `clean` deletes the classes under it. A plain `SPRING_PROFILES_ACTIVE=test ./gradlew test` here is fine.

## Static checks

The IDE's Jmix inspection does not resolve the actions of the project's own `@ActionType`s. On an `@Install` to such an action, such as `assignmentsDataGrid.create` in the user editor, it reports "Unable to find installation point for delegate" and "Unable to find specified ui component" as errors, and the method as never used. These are false: the delegate binds at runtime, and a UI test that opens the view and relies on the delegate proves it.

## Local overrides in `.jmix/conf`

Jmix reads a resource from `jmix.core.conf-dir` (`.jmix/conf/` in the project directory by default) before the classpath, both at runtime and in tests. While Jmix Studio is open, its hot deploy writes changed resources there, and compiled classes too; the directory is gitignored. A copy there wins over the checked-out file even when it is stale: after a branch switch, or after an edit the copy does not have yet. Typical symptoms:

- `NoSuchViewException: View '<id>' is not defined` while the menu is built, from a stale `menu.xml` that names a view the current branch does not have;
- a raw message key, or an old text, from a stale copy of a message bundle;
- `ClassCastException` between a class and itself, or `IllegalAccessError` on a package-private class, from a hot-deployed `.class`: the tests load it through Jmix's own class loader, apart from the application's classes.

Fix: delete the stale file, or the `.class`, under `.jmix/conf/`; Studio writes a class there again on the next save of its source. CI never has this directory.

## Live checks

Start the application with the `dev` profile for demo data (the users are in `demo/DemoDataLoader.java`, and each one's password is their username); the render walk is described in the `jmix-verify-bootrun` skill. Only a live check can verify:

- server-push updates, such as the rejected-entries badge refreshing in another user's open session;
- how screens look: badge colors, icons, card layouts, and columns fitting their content after rows change;
- file uploads: the file picker, drag and drop, and the transfer itself; UI tests run without a browser, so they reach an upload field only through its server side.

A running app picks up Java changes only after a restart; descriptor and CSS changes reach it after `./gradlew processResources`, then a logout and login, because in tabbed mode a plain reload keeps the old view instance. The `live-checker` agent runs a live check against the app the developer has running.
