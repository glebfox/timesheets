# UX

How a screen helps its user understand it: the hints it gives and the names of its controls and progress bars. How a screen looks is `design-system.md`.

## When a hint earns its place

A hint, a tooltip or a helper text, is added only where it really helps, never for the sake of having one. It helps where:

- the screen hides a rule that changes the result, such as the forms the time entry editor's spent time accepts, or what a task's required tag groups demand of its time entries;
- a field is locked and nothing on the screen says why, such as the time entry editor's task field before a user is chosen;
- an element does not say what it is, such as a red count with no label beside it.

Not every field needs one: a status, a holiday calendar's day type, a user's Active switch, and a user's time zone have none.

A hint explains before the user acts. One whose meaning shows only once a value is chosen comes too late.

## Tooltip or helper text

A form does not change its height as the user fills it in:

- A hint that comes and goes with a value or a lock is a tooltip, such as the time entry editor's "Choose a user first" on the locked task field.
- A helper text under a field, or a muted line under a section's heading, is always there, such as the spent time's "Such as 1:30 or 1h 30m", or set by what the editor opened on, such as the line in the default schedule's own editor that says users with no assignments work to it.

Older fields predate the rule: the date range field's summary line appears once dates are chosen, and the "This … is deleted" helpers of a deleted holiday calendar, schedule, or absence type go once another value is chosen.

A tooltip is a Vaadin tooltip, never a `title` attribute, which the browser draws in its own style and after its own delay, so that every hint on a screen looks and opens alike. How a component without a tooltip of its own, such as a badge, takes one: `views.md`. The calendar's events are the exception: the FullCalendar add-on shows an event's description, such as an entry's description, an absence's type and dates, or a holiday's name, as a `title`.

## Buttons with only an icon

A button that shows an icon and no text has a tooltip that names what it does, such as the period arrows of the calendar and of the approval screen, and the approval board's Approve day: an icon alone does not say for certain what the button does, and the tooltip is the only text the button has.

## Progress bars

Every progress bar has an accessible name, in the user's language, that says what it measures, such as the calendar's month bar, "Logged of the month target"; a bar that repeats on a screen also says which one it is, as each of the calendar's week cards does with the week's dates. An endless bar, such as the login page's while the demo data loads, takes the name of what it waits for, there its dialog's title. ARIA requires a name on an element with the `progressbar` role, which Vaadin's progress bar has: without one, a screen reader announces only "progress bar" and a value, and accessibility checkers report it. Vaadin's `ProgressBar` has no setter for the name, so it is the element's `aria-label`.
