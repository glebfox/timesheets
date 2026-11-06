# Views and actions

Mechanics of the framework's views, grids, filters, list actions, components declared in descriptors, custom fields and filters, and tooltips that the project's own views, actions, and roles are built on, and that the project skills do not state. How to write a view in general is the `jmix-create-*` skills; no skill covers custom list actions, whose rule is in `docs/conventions.md`.

## Read-only detail views

A detail view of a saved entity opens read-only when the current user may not update that entity: no entity-level update permission, or a row-level constraint that refuses it. The read-only mode changes two things only:

- Fields bound to an entity attribute become read-only. A field the view fills or builds itself, such as the work schedule editor's workday buttons or the time entry editor's tag fields, does not follow the mode: the view sets it on each change of the mode, and on a field it builds later, such as a tag field rebuilt for another task, when it builds it.
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

The standard create and edit actions do not invoke their `afterSaveHandler` when the detail view opens by navigation, as the editors opened from this project's lists do unless a list says otherwise; the framework only logs that the handler is set but not invoked. In tabbed mode the list stays open, and a save reaches it only as the saved row added to its container or replaced there. A list whose actions open the editor as a dialog (the action's `openMode` property set to `DIALOG`), as the absence list does, gets the saved row the same way, and there the actions also invoke `afterSaveHandler`. A list that shows a value its own query does not load, as the holiday calendar list shows Filled until, listens for that change of its container and loads the value for the rows added or replaced.

## Grids whose rows change after they are shown

The browser sizes an `autoWidth` column once, to the rows it shows first. Rows that come later, whether added, replaced, or loaded again, and a badge that later appears on a row already shown, can be wider, and the column does not grow to fit them. A grid whose rows can change after it is shown calls `recalculateColumnWidths()` on those changes of its container: the work schedule list on every change, and the assignment and day tables of the editors when items are added or replaced. An `autoWidth` column that should keep its content width also has `flexGrow="0"`; otherwise it takes a share of the free width.

A replaced item redraws only its own row. A cell computed from other rows as well, such as the Current badge of the assignment tables, would keep its old content on the rows not replaced, so those grids call `setRefreshAllOnItemReplace(true)` on their `ContainerDataGridItems`, and a replace redraws every row.

## Grids whose every column security hides

When security hides every property column of a grid, because the user may view none of the attributes it shows, the framework puts its own empty state in the grid, a warning icon and "Access denied" («Доступ запрещен»), in place of the empty-state component or text that the descriptor sets. So a screen with a grid of an entity a role does not read shows "Access denied" there, not the grid's own empty text, as the user editor's Work schedule section does for a holder of User: read only (`docs/specs/views/users.md`). The project's `gridEmptyState` is set as the grid's `emptyStateComponent`, so the framework's state still takes its place.

## Input dialogs that are not detail views

Only a detail view checks for unsaved changes when it closes. A dialog that is a plain `StandardView` and collects input for its opener closes at once on Cancel, Esc, or its close button. The calendar days dialog and the add users dialog ask the framework's unsaved-changes question through `UnsavedInputSupport` in `app/util/`: each compares its fields with how it opened and passes the result from its `BeforeCloseEvent` handler. A close with `StandardOutcome.SAVE`, and a close that does not check for unsaved changes, such as one with `StandardOutcome.DISCARD`, pass without the question.

A side dialog, such as the reject dialog of the approval screen, is a component of the view that opened it, not a view, so it asks through `UnsavedInputSupport.askBeforeClosing(sideDialog, owner, inputChanged, focusOnKeep)`, which closes the dialog at once when nothing changed and otherwise asks the question and closes it on Yes. The framework's No focuses the first focusable component of the owner view, behind the dialog, so the method then focuses `focusOnKeep`, a field of the dialog. Its Cancel button calls it, and so must its close-action listener, set with `withCloseActionListener`: once a listener is set, Esc and a click outside no longer close the dialog on their own, so the listener routes both.

## URL query parameters in tabbed mode

In this app the `urlQueryParameters` facet does nothing: the tabbed-mode add-on registers `NoopUrlQueryParametersFacetImpl` in place of the framework's facet, so a list's filter and pagination are not kept in the URL. A reload of the browser page keeps them all the same, since tabbed mode restores the open view; the browser's back and forward buttons create the view again, with an empty filter. The list descriptors keep the facet for when the add-on supports it.

## Generic filters for users who may not configure them

A `genericFilter` shows Add condition and its settings button only to a user with the `ui.genericfilter.modifyConfiguration` specific policy, which among the project's roles only Full access grants. Anyone else can use only the configurations the descriptor declares, so a generic filter that other roles use declares a default configuration with the conditions they need, as the work schedule list does with its name condition (`docs/specs/views/settings.md`).

## Components declared with the generic element

A component of the project's own that has no element of its own in the descriptors, such as the approval screen's person panel and basket bar, is declared with the generic `<component id="…" class="…"/>` element. The loader creates it through `UiComponents`, and so through Spring: the component has a constructor without arguments and takes its beans as `AvatarUpload` does, as `ApplicationContextAware` and `InitializingBean`, building the parts that need them in `afterPropertiesSet`. What it shows comes through its own methods once it is created, such as the person panel's `show` and the basket bar's `setEntries`, never through a constructor. The loader sets the `id`, `visible`, and `css` attributes and the `properties` element, not `classNames`, so the view's stylesheet reaches such a component by its own class.

The view wires it as it wires a framework component. `@Subscribe("<id>")` finds the public `addXxxListener(ComponentEventListener<E>)` that returns a `Registration` by the event type of the method's parameter alone. When two such methods take the same event type, `subject` must name one by its full method name, `addXxxListener`; only for a `ClickEvent` does the framework add the `add` itself, so there the subject is `xxxListener`, as with a button's `clickListener`. `@Install(to = "<id>", subject = "xxx")` reaches a public `setXxx` that takes one functional interface.

## Custom fields and filters

The date range field and its filter (`component/daterange/`, [decision 0015](../decisions/0015-a-range-of-two-dates-is-one-field-of-its-own.md)) rely on mechanics of Jmix's own components that the project skills do not state.

- A field that is not an `EntityValueSource` binding gets from the framework no required flag, no bean validation, and no attribute-permission check. The view's read-only mode reaches only a component that is `SupportsValueSource` with a value source, and checks attribute permissions only for an `EntityValueSource`. A custom field bound to the data model therefore goes through a value source of its own that checks `UiEntityAttributeContext` in `isReadOnly()`, and the field sets its own required mark. A value source fires no value change for its own writes, or the binding reads the value back into the field.
- A view refuses the field with `setInvalid(true)` and `setErrorMessage(...)`, in either order: `setInvalid(true)` runs no validators, so it leaves the message alone. Only `setInvalid(false)` runs the validators again, which is how the field drops both the mark and the message at the next change of either date.
- A filter on `SingleFilterComponentBase` applies only a change that comes from the client, so a value set from the server loads nothing; a value the filter must not use is held back in both `onFilterValueChanged` and `updateQueryCondition`. Its condition is built before the loader has set an id or a property, so the label's id and the parameter names are set later, and every parameter needs a name or the first load fails. Its value field is a nested element of the filter's own element, and a filter element is never declared inside `urlQueryParameters`, whose loader fails on a child no binder supports.

## Tooltips that render Markdown

`Tooltip.forComponent(part).setMarkdown(text)` renders the text as Markdown in a `vaadin-tooltip` that Vaadin attaches to `body`, outside the component, so only a theme-wide rule reaches it: the theme's `component/tooltip.css` trims the browser's margins above its first block and below its last. A tooltip stays open while the pointer is on it and hides what it covers, so one on a part with buttons nearby opens where it covers none of them, as a week board card's description opens beside the card with `TooltipPosition.END_TOP`. A part that takes no focus shows its tooltip on hover only, never to the keyboard. See `ApprovalEntryCard`.

A grid gives a cell its tooltip from the column's `tooltipGenerator`, an `@Install` delegate, and renders the tooltips as Markdown only after the controller calls `setTooltipMarkdownEnabled(true)` on the grid: the descriptor has no attribute for it. A grid cell takes keyboard focus, so its tooltip opens on focus as well as on hover. See `MyTimeEntryListView` and `TimeEntryListView`.
