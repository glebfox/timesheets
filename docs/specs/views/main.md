# Main view, login, and error pages

Source: `view/main/`, `view/login/`, `menu.xml`, `security/UiMinimalRole.java`

The application frame around every screen, the login page, and the error pages.

## Main view

The frame of the signed-in application: the drawer, with the logo at its top, the menu, and the user menu at its foot, and beside it the work area, with no bar above either. Screens open as tabs in the work area; tabs can be dragged, and each offers closing itself, the others, or all. Why there is no bar across the top and the menu toggle is in the work area: [decision 0011](../../decisions/0011-the-main-view-has-no-navigation-bar.md).

- **Logo** — the Timesheets logo without the tagline heads the drawer, centered, drawn for light surfaces in the light theme and for dark surfaces in the dark one, whether the theme is picked in the user menu or follows the system; its alternative text is "Timesheets". It is as tall as the tab bar, and the line under it continues the tab bar's.
- **Menu toggle** — opens and collapses the drawer. It is at the start of the tab bar, before the tabs, and in the same place on the empty work area while no tab is open, on a touch screen too.
- **Menu** — Timesheets (Calendar, My time entries, My tasks), Project Management (Clients, Projects, Time entries, Approve time entries), Settings (Task types, Project roles, Work schedules, Holiday calendars), and Administration (Users) start expanded; Security, Data Tools, and Pessimistic locking hold the framework's own screens and start collapsed. Every group and item shows an icon of its own, and no two are alike. A user sees only the items their roles allow. The menu scrolls on its own, with a thin scrollbar, while the logo above it and the user menu below it stay in place.
- **Rejected badge** — the My time entries item carries a red badge with the number of the current user's Rejected [time entries](../data-model/time-entry.md#status), hidden when there are none. It refreshes while the user is signed in, whenever one of their entries changes status or is deleted, by anyone and from any session.
- **User menu** — at the foot of the drawer, across its width, so while the drawer is collapsed it is reached by opening the drawer. The button shows the user's avatar and full name (the username when both names are empty), cut short when it does not fit, and marks a substituted user. The menu it opens is at least as wide as the button; its header shows the name with the username beneath it. Its items are Substitute user, the theme switch, and Log out.

## Login

`login`. A form with username, password, "remember me", and a language choice of English or Russian that starts at the browser's language; there is no "forgot password". The `dev` profile pre-fills `admin`/`admin`. A failed sign-in shows the form's error without saying why: wrong credentials, an inactive or locked user, and a user without UI access look alike.

## Error pages

An unknown address followed within the application without reloading the page, such as through a link to a screen that no longer exists, shows Vaadin's default page, "Could not navigate to" the address, which in development mode also lists the application's routes. An unknown address typed into the browser never reaches the application: a signed-in user gets the server's plain error page (Spring Boot's Whitelabel Error Page) with status 403, and a signed-out user is sent to the login page. Why such addresses stay forbidden: [decision 0005](../../decisions/0005-addresses-that-open-no-view-stay-forbidden.md).

A screen the user may not open does not open, whether its address is followed or typed into the browser: an "Access denied" notification is shown instead, over the main view when the address was typed.

## Not covered

- The live refresh of the rejected badge: it needs two live sessions (`docs/guidelines/testing.md`).
- What an address typed into the browser gets before any screen opens, the 403 and the redirect to the login page: no test sends a request through the security filters.
- How the menu's icons look: `MenuIconsTest` checks only that each group and item names an icon of its own that the menu's set defines, so only a live check sees them (`docs/guidelines/testing.md`).
- How the frame looks, which no test sees (`docs/guidelines/testing.md`). A live check on 2026-11-03, on `dev` demo data, as admin, in English, in the light and the dark theme, at 1280×800 and at 375×812 with a touch screen emulated, with the drawer open and collapsed, looked at the logo in each theme and as the system theme changed, the menu toggle keeping its place as the first tab opened and the last one closed, the line under the logo meeting the tab bar's, the user menu across the drawer with a long name, set in the browser, cut short, and the menu it opens, and the menu's thin scrollbar. Russian, a substituted user, and a theme picked in the user menu were not looked at.
- The rest of these screens: no UI test exists for them.
