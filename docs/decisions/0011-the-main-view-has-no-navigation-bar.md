# 0011 The main view has no navigation bar

Status: accepted

## Context

Jmix's main view puts the drawer toggle, the application title, and the user menu in a navigation bar above the work area, so the tab bar sat below a second bar. Issue #21 asked to remove that bar, move the user menu to the foot of the drawer, put the logo at the top of the drawer as the title, and find a place for the drawer toggle.

Alternatives considered and rejected:

- **Keep the navigation bar, as Jmix's template has it.** It is a second bar above the tab bar, for three controls that the drawer and the tab bar can hold.
- **The toggle beside the logo at the top of the drawer, and one in the work area only while the drawer is collapsed.** It was built on the branch of issue #21 and replaced before the merge. The work area needs a toggle of its own anyway, in the tab bar and in the initial layout, for when the drawer is collapsed, so the drawer's toggle only duplicated it. Each toggle also hid once it was clicked, so the focus was lost unless a script of the project's own moved it on to the toggle shown in its place, after every close of an overlay drawer as well, since an overlay drawer gives the focus to the first toggle when it closes.

## Decision

The main view declares no navigation bar. The drawer starts with the logo and ends with the user menu. The drawer toggle is at the start of the main tab sheet's tab bar and in the initial layout that shows while no tab is open, always visible, and the two pad alike, so the toggle keeps one place. Decided by the user (issue #21).

## Consequences

- The toggle that is clicked stays where it is, so the focus stays on it with no script of the project's own.
- The user menu, Log out included, is reached only with the drawer open.
- The IDE's descriptor inspection reports `appLayout` as incomplete without `navigationBar`, which both main view XSDs declare as required; the error is false, and a comment in `main-view.xml` says so.
- A screen gets the height the navigation bar took.
