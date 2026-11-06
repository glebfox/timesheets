# Screens

Source: `component/emptystate/`, and the `gridEmptyState` element of each view descriptor under `view/`

What every screen with a list or a table shares. Each screen, or each functional area of screens, has its own spec in this directory and names there what is its own, such as the icon and the text of each of its empty grids.

## Empty lists and tables

Every list, picker, and table of a screen shows an empty state while it has no rows: an icon beside a line that says what is missing, such as "No projects" («Нет проектов»), both in the same muted color and centered in the grid. A filter that matches nothing reads the same as a list with nothing in it. The icon is the main menu's icon of the records the grid lists; tags and tag groups, which have no menu entry, have icons of their own in the same style.

A list or a picker, which loads its own data, also shows Refresh under the line. Refresh loads the list again with its filters as they are. A Refresh that brings rows moves the keyboard focus to the list's first cell; one that brings none leaves it on Refresh. Refresh is not a Tab stop of its own: Tab reaches the grid's empty cell, and Enter on it reaches the button. No toolbar has a Refresh button. A list's right-click menu shows Refresh as its last item, also where a list serves as a picker, such as the client list; a picker of its own, such as the tag picker, has no right-click menu.

A table in an editor shows no Refresh: its rows are edited with the edited record and saved by its OK. Why Refresh is offered where it is: [decision 0017](../../decisions/0017-refresh-lives-in-the-empty-state-not-in-the-toolbar.md).

When security hides every column of a grid, the framework's warning icon and "Access denied" («Доступ запрещен») take the empty state's place ([views guideline](../../guidelines/views.md#grids-whose-every-column-security-hides)).

## Not covered

- How the empty states look and behave in a browser, which no test sees (`docs/guidelines/testing.md`). A live check on 2026-11-06, on a list, a picker, a composition table in an editor, and an assignment table in a new user's editor, in the light and the dark theme, in English and on a Russian screen, saw the following:
  - the icon and the text on one line, centered, in the same muted color, with Refresh under them as a tertiary button with the refresh icon, and the Russian texts whole;
  - the tag and tag group icons matching the menu's;
  - Refresh loading a row saved in another session while keeping the applied filter, and a tab restored after a reload of the browser page still offering one working Refresh;
  - Refresh as the last item of the lists' right-click menu, and no right-click menu on the pickers;
  - the keyboard and the focus after Refresh, as described above.
- The focus after a Refresh that brings rows relies on an internal of the browser's grid component, so an upgrade of Vaadin can silently stop it; only a live check would notice.
- What the tests check: each grid's icon, English text, and whether it offers Refresh (`GridEmptyStatesUiTest`); that every grid has an empty state whose icon is in the set and whose text is a key in both bundles (`GridEmptyStateDescriptorsTest`); and, on a test view, that Refresh loads new rows, follows its action's state, exists only while the empty state has an action, asks the browser to move the focus, and gives way to "Access denied" (`GridEmptyStateUiTest`). The Russian texts are checked by no test.
