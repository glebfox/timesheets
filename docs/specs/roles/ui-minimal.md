# UI: minimal access

Source: `security/UiMinimalRole.java`, `demo/DemoDataLoader.java`

Resource role `ui-minimal`, named "UI: minimal access", with UI scope.

## Grants

- Signing in to the UI (the `ui.loginToUi` permission).
- The login and main views.
- The framework's system dialogs, such as changing one's own password and substituting a user.

It grants no entity access and no other view, so on its own a user can sign in and see the main view with the [dashboard](../views/dashboard.md)'s header alone: its My time is for a user who may log time. It is meant to be combined with roles that grant the data and screens a user needs, such as [Work schedule management](work-schedule-management.md) or roles created at runtime.

## Holders

None directly. [Worker](worker.md) and [Observer](observer.md) include it, and through Worker every other job role, so in the `dev` demo data every user but `admin` holds it.

## Not covered

- What the role grants: signing in is checked only through the job roles that include it, each held alone (`JobRolesTest`); the main and login views and the system dialogs are not. `DashboardUiTest` only shows the dashboard to a holder of this role alone, without My time and Log time.
