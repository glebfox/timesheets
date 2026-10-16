# Main view, login, and error pages

Source: `view/main/`, `view/login/`, `router/error/`, `menu.xml`, `security/UiMinimalRole.java`

The application frame around every screen, the login page, and the error pages.

## Main view

The frame of the signed-in application. Screens open as tabs in its work area; tabs can be dragged, and each offers closing itself, the others, or all.

- **Header** — the menu toggle, the "Timesheets" title, and the user menu. The user menu button shows the user's avatar and full name (the username when both names are empty), and marks a substituted user. Its header shows the name with the username beneath it. Its items are Substitute user, the theme switch, and Log out.
- **Menu** — Timesheets (My time entries, My tasks), Project Management (Clients, Projects, Time entries), Settings (Task types, Project roles), and Administration (Users) start expanded; Security, Data Tools, and Pessimistic locking hold the framework's own screens and start collapsed. A user sees only the items their roles allow.
- **Rejected badge** — the My time entries item carries a red badge with the number of the current user's Rejected [time entries](../data-model/time-entry.md#status), hidden when there are none. It refreshes while the user is signed in, whenever one of their entries changes status or is deleted, by anyone and from any session.

## Login

`login`. A form with username, password, "remember me", and a language choice of English or Russian that starts at the browser's language; there is no "forgot password". The `dev` profile pre-fills `admin`/`admin`. A failed sign-in shows the form's error without saying why: wrong credentials, an inactive or locked user, and a user without UI access look alike.

## Error pages

An unknown address shows a "Nothing was found" page and a screen the user may not open shows an "Access denied" page, each with a link back to the application.

## Not covered

- The live refresh of the rejected badge: it needs two live sessions (`docs/guidelines/testing.md`).
- The rest of these screens: no UI test exists for them.
