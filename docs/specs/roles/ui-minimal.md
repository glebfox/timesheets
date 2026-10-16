# UI: minimal access

Source: `security/UiMinimalRole.java`

Resource role `ui-minimal`, named "UI: minimal access", with UI scope.

## Grants

- Signing in to the UI (the `ui.loginToUi` permission).
- The login and main views.
- The framework's system dialogs, such as changing one's own password and substituting a user.

It grants no entity access and no other view, so on its own a user can sign in and see an empty main view. It is meant to be combined with roles created at runtime that grant the data and screens a user needs.

## Holders

None in code or seed data.

## Not covered

- What the role grants: no test checks it.
