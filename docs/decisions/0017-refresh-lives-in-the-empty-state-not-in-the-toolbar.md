# 0017 Refresh lives in the empty state, not in the toolbar

Status: accepted

## Context

Issue #42 gave every grid an empty state that names what is missing. An empty list gave no way to load again data that someone else may have added since it opened. The lists had no Refresh button.

Alternatives considered and rejected:

- **A Refresh button in the toolbar of every list.** The user kept the toolbars as they are and offered Refresh where a list needs it, when it is empty; a list with a generic filter already reloads through the filter's own Refresh button.
- **Refresh on the editors' tables too.** Their rows are edited in the editor's data context and saved by OK, so a reload would bring back rows the user removed while the context still removes them on save, and a new user's or schedule's assignment table has no parameter to load by. In concept they are compositions of the edited record (decided by the user).
- **A text of its own for a list that a filter empties, and a hint line.** A loader's condition cannot tell a user's filter from a view's own query condition, as in the task and tag group pickers; the user chose one text for every empty list.
- **Refresh left out of the lists' right-click menu.** The framework adds every grid action to that menu and can switch the menu off only for all of a grid's actions; leaving Refresh out would take code that works against it. The user chose to keep it.

## Decision

Every grid shows `gridEmptyState`: an icon and "No <things>", the same whether or not a filter is applied. A list or a picker, which loads its own data outside an editor, declares the standard `list_refresh` action, and its empty state shows Refresh bound to it. No toolbar has a Refresh button, and a list's right-click menu shows Refresh last. A table in an editor gets no Refresh. Decided by the user (issue #42).

## Consequences

- A list with rows is loaded again from its right-click menu, or from its generic filter's Refresh where it has one; a picker with rows has no Refresh.
- A new list or picker declares `list_refresh`; a new editor table does not.
