# Filters: own configurations

Source: `security/OwnFiltersRole.java`

Resource role `own-filters`, named "Filters: own configurations", with UI scope.

## Grants

- In a list's generic filter, Add condition and the settings button, so a holder adds conditions on the attributes the filter offers, groups them, and saves, changes, and removes configurations of their own (the `ui.genericfilter.modifyConfiguration` permission).
- The filter's dialogs: adding a condition, the date interval of a condition, and the editors of a property condition and of a group of conditions; every operation on saved filter configurations and on the conditions they hold, with every attribute modifiable.

It is the framework's "UI: edit filters" without two of its grants, which only [Full access](system-full-access.md) keeps:

- Configurations for everybody (`ui.genericfilter.modifyGlobalConfiguration`): a holder sees the configurations made for everybody and applies them, but cannot make one, save or remove one, or make a configuration the default for everybody; the configuration editor of such a configuration opens read-only for them.
- JPQL conditions (`ui.genericfilter.modifyJpqlCondition`, their entity and their editor): adding a condition does not offer them, since a JPQL condition is query text that its author writes themselves.

The filter shows a holder only their own configurations and those for everybody, but the data layer does not narrow configurations to a holder's own: code running as a holder, outside the filter, could read and change other users' configurations.

It grants no signing in and no list: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md), and the roles of the lists they filter. A user without this role or Full access uses only the configurations a list's descriptor declares (`docs/guidelines/views.md`).

## Holders

None in code or seed data.

## Not covered

- What the generic filter shows a holder: the role test checks the permissions, views, and entities the filter and its dialogs ask about, not the filter itself. No test adds a condition, saves a configuration, or opens a configuration for everybody as a holder.
- A holder who lacks UI: minimal access: not checked.
- Reading or changing another user's configuration by code running as a holder: no test.
