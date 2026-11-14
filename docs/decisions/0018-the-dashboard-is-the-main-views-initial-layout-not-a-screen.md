# 0018 The dashboard is the main view's initial layout, not a screen

Status: accepted

## Context

While no tab was open, the work area held only the menu toggle. Issue #69 fills that space with what a user wants to know when they sign in or close their last tab: their own time, and for an approver, their team's.

Alternatives considered and rejected:

- **A screen of its own, with a menu item and an address.** It would be one more tab among the others, and the work area would stay empty whenever no tab is open.

## Decision

The dashboard is a fragment that the main view's initial layout holds, under the menu toggle, so it shows whenever no tab is open: after sign-in and after the last tab closes. It has no menu item and no address, and as part of the main view it needs no permission beyond UI: minimal access. The fragment loads all its own data, so it can become the content of a screen later without change. Decided by the user (issue #69).

## Consequences

- A user returns to the dashboard by closing every tab; nothing opens it as a tab.
- It reloads each time it is shown again and loads nothing while a tab hides it, so it never follows changes made while it is shown.
- A main view opened on a tab, as by a link to a screen, shows the dashboard only after that tab closes.
