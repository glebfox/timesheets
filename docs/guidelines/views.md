# Views and actions

Mechanics of the framework's views, grids, filters, and list actions that the project's own views, actions, and roles are built on, and that the project skills do not state. How to write a view in general is the `jmix-create-*` skills; no skill covers custom list actions, whose rule is in `docs/conventions.md`.

## Read-only detail views

A detail view of a saved entity opens read-only when the current user may not update that entity: no entity-level update permission, or a row-level constraint that refuses it. The read-only mode changes two things only:

- Fields bound to an entity attribute become read-only. A field the view fills itself, such as the work schedule editor's workday buttons, does not follow the mode unless the view sets it on each change of the mode.
- Component actions that implement `AdjustWhenViewReadOnly`, and return `true` from `isDisabledWhenViewReadOnly()`, are disabled.

OK is not such an action, and saving has no read-only check, so OK still saves the view's data context. An action that must keep working in a read-only editor does not implement `AdjustWhenViewReadOnly` and relies on its own permission check; OK then saves only what such actions changed. The assignment actions of the user editor and the work schedule editor work this way, so a holder of Work schedule management changes assignments in a user editor that is read-only for them (`docs/specs/roles/work-schedule-management.md`).

## List actions in an editor

In a detail view, a table loaded by its own query, rather than a property container of a composition attribute, does not share the editor's data context with the standard list actions:

- `list_remove` deletes the selected rows from the database at once, before the editor's OK.
- `list_create` and `list_edit` save their dialog to the database at once, and the editor's Cancel does not undo it (the `jmix-add-dialog-detail-flow` skill, "Whose data context does the dialog use").

Such a table uses custom actions instead, as the assignment tables of the user editor and the work schedule editor do. Create and edit open their dialog with the editor's data context as its parent (`withParentDataContext`), and remove takes the items out of the container and removes them in the data context, so the editor's OK saves every change and its Cancel discards it. A table of a composition, such as a holiday calendar's days, works with the standard actions, whose changes wait for the editor's OK.

## Base classes of custom list actions

`SecuredListDataComponentAction` is enabled only with a selected item, and its permission check is the row-level constraint of its entity operation on that item: it does not check the entity-level permission. An add action built on it could never be used on an empty table. So a custom list action:

- that needs no selection, such as a create or an add, extends `ListDataComponentAction`, as the framework's `CreateAction` does, and checks the entity-level create permission;
- that works on selected items extends `SecuredListDataComponentAction` with its entity operation, and checks the entity-level permission of that operation before the row-level one.

## Lists after an editor save

The standard create and edit actions do not invoke their `afterSaveHandler` when the detail view opens by navigation, as the editors opened from this project's lists do; the framework only logs that the handler is set but not invoked. In tabbed mode the list stays open, and a save reaches it only as the saved row added to its container or replaced there. A list that shows a value its own query does not load, as the holiday calendar list shows Filled until, listens for that change of its container and loads the value for the rows added or replaced.

## Grids whose rows change after they are shown

The browser sizes an `autoWidth` column once, to the rows it shows first. Rows that come later, whether added, replaced, or loaded again, and a badge that later appears on a row already shown, can be wider, and the column does not grow to fit them. A grid whose rows can change after it is shown calls `recalculateColumnWidths()` on those changes of its container: the work schedule list on every change, and the assignment and day tables of the editors when items are added or replaced. An `autoWidth` column that should keep its content width also has `flexGrow="0"`; otherwise it takes a share of the free width.

A replaced item redraws only its own row. A cell computed from other rows as well, such as the Current badge of the assignment tables, would keep its old content on the rows not replaced, so those grids call `setRefreshAllOnItemReplace(true)` on their `ContainerDataGridItems`, and a replace redraws every row.

## Input dialogs that are not detail views

Only a detail view checks for unsaved changes when it closes. A dialog that is a plain `StandardView` and collects input for its opener closes at once on Cancel, Esc, or its close button. The calendar days dialog and the add users dialog ask the framework's unsaved-changes question through `UnsavedInputSupport` in `app/util/`: each compares its fields with how it opened and passes the result from its `BeforeCloseEvent` handler. A close with `StandardOutcome.SAVE`, and a close that does not check for unsaved changes, such as one with `StandardOutcome.DISCARD`, pass without the question.

## Generic filters for users who may not configure them

A `genericFilter` shows Add condition and its settings button only to a user with the `ui.genericfilter.modifyConfiguration` specific policy, which among the project's roles only Full access grants. Anyone else can use only the configurations the descriptor declares, so a generic filter that other roles use declares a default configuration with the conditions they need, as the work schedule list does with its name condition (`docs/specs/views/settings.md`).
