# UI: minimal access

Source: `security/UiMinimalRole.java`, `demo/DemoDataLoader.java`

Resource role `ui-minimal`, named "UI: minimal access", with UI scope.

## Grants

- Signing in to the UI (the `ui.loginToUi` permission).
- The login and main views.
- The framework's system dialogs, such as changing one's own password and substituting a user.

It grants no entity access and no other view, so on its own a user can sign in and see the main view with the [dashboard](../views/dashboard.md)'s My time: their plan, absences, and holidays, which every user sees, and none of their time entries. It is meant to be combined with roles that grant the data and screens a user needs, such as [Work schedule management](work-schedule-management.md) or roles created at runtime.

## Holders

- The demo user `mossman`, in the `dev` demo data only.

## Not covered

- What the role grants: no test checks it; `DashboardUiTest` only shows the dashboard to a holder of this role alone, without Log time.
