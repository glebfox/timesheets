# Absence screens

Source: `view/absence/`, `app/AbsenceSupport.java`, `component/daterange/`, `app/util/ComponentSupport.java`, `app/util/DateRangeFormatter.java`, `app/util/PluralForms.java`

The absence list and the absence editor, under the Administration menu after Users. What an absence is: [absence](../data-model/absence.md); who opens these screens: [Absence management](../roles/absence-management.md). Absence types are under Settings ([settings](settings.md#absence-types)).

## Absences

`absences`. Lists every user's absences, newest first by first day, with multiple selection:

- **User** — the user cell: avatar, full name or username, and an Active or Inactive badge.
- **Type** — the type's name, with a neutral Deleted badge when the type is deleted. Absences of a deleted type are listed like any other, also when the list is sorted by type.
- **Dates** — `<first day> – <last day>`, such as "02/03/2026 – 13/03/2026", or one date for a one-day absence.
- **Days** — the number of calendar days, weekends and holidays included, in the form the number takes, such as "1 day" or "14 days" («1 день», «3 дня», «14 дней»). It is not the number of workdays the absence takes from the plan.
- **Comment** — cut short, with the full text in a popover.

Filters:

- **User** — a drop-down of users, inactive ones included, searched by username, first name, or last name, case-insensitively, with a picker that opens the [user list](users.md#users) and a button that clears it.
- **Type** — a drop-down of the non-deleted types, searched by name, with a picker that opens the [absence type list](settings.md#absence-types) and a button that clears it.
- **Period** — a From and a To date side by side, joined by a dash, each named "From" or "To" for screen readers, each with a clear button, and either optional. They keep every absence sharing at least one day with the period, both of its ends included: an absence that ends on From or starts on To is kept. The To picker offers no day before From. A date picked applies at once. A To typed before From is refused under the dates with "The last day must not be before the first day" («Последний день не может быть раньше первого») and not applied: the list stays as it was, with the last valid period, until the period is valid again.

The user, type, and period filters take columns of one width: three in a row on a wide screen, as at 1440px, two on a narrower one, as at 1280px with the menu open, and one on a narrower one still, as at 600px and on a phone. The period's two dates share its column evenly, side by side, and stack, each as wide as the column with the dash centered between them, when the column is too narrow for two filled dates whole: at a 375px window, and side by side from 480px up.

The filters and pagination are kept when the browser page is reloaded; opening the screen again, or returning to it with the browser's back or forward button, starts them empty.

Actions: Create, Edit, and Remove. Create and Edit open the [absence editor](#absence-editor) as a dialog over the list, and the list shows what it saves at once: a created absence among the rows, an edited one changed in place. Remove asks the usual confirmation and deletes every selected absence.

## Absence editor

`absences/:id`, titled "Absence", a 480px dialog when opened from the list.

- **User** — required: a drop-down of users, inactive ones included, searched by username, first name, or last name, case-insensitively, with a picker that opens the [user list](users.md#users) and a button that clears it.
- **Type** — required: a drop-down of the non-deleted types, searched by name, with a picker that opens the [absence type list](settings.md#absence-types) and a button that clears it. An absence of a deleted type shows that type with the helper "This type is deleted" and saves as it is; choosing another type drops the helper.
- **Dates** — required: one field named "Dates" with the first day, a dash, and an optional last day, whose pickers are named "First day" and "Last day" for screen readers. The last day's picker offers no day before the first and, while the last day is empty, opens at the first day's month the first time it opens in the editor; opened again, it keeps that month, even after the first day changes. A line under them names the range as it changes, such as "14 days, Monday to Sunday" («14 дней, с понедельника по воскресенье»), or "1 day, Monday" («1 день, понедельник») for one day. An empty last day saves a one-day absence, and a one-day absence opens with its last day empty, so it stays one day when its first day changes. A last day typed before the first is refused with "The last day must not be before the first day" («Последний день не может быть раньше первого»), one message under both dates in place of the line. There is no limit on the length. An empty first day is marked "Dates required" («Заполните поле "Даты"»), and a new absence's editor opens with the field marked so, as with User and Type.
- **Comment** — a plain text area, five rows tall.

Saving is refused, with both dates marked as one field and the message under them, below the range line, when the range shares a day with another non-deleted absence of the same user, whatever its type, a deleted type included: "Overlaps an absence: 02/03/2026 – 13/03/2026 Vacation" («Пересекается с отсутствием: …»), naming every such absence. A range that starts the day after another absence ends, or ends the day before it starts, does not overlap it. The check sees all of the user's absences, whatever the saving user may read. Changing the user or either date clears the refusal.

Closing the editor with unsaved changes, with Cancel, Esc, or the dialog's close button, asks the framework's usual question first. In a read-only editor, as for a user who may not change absences, neither date can be changed.

## Not covered

- How the screens look, in English and Russian, light and dark: the list's columns, the Days forms, the comment popover, and the Deleted badge; the filters' columns, three at 1440px, two at 1280px with the menu open, and one at 600, 480, and 375px, with no horizontal scroll and every filled date whole, the period's dates side by side at 480px and above, stacked at a 375px window, and the editor's dates side by side down to a 375px window; the editor as a dialog, its comment five rows tall, its range line, and the last day's picker greying out the days before the first. Checked live on 2026-11-04, and again on 2026-11-05 for the date range field: the editor and the days dialog never stack at 1440, 1280, 600, 480, and 375px, and the period stacks only at a 375px window, side by side at 480, 768, and 1024px. The UI tests check the list's order, renderers, and cell texts in English, the badge's text and style, the period dates' screen reader names, and the last day's minimum; the Russian forms of Days and of the range line are tested in `DateRangeFormatterTest`.
- The month the last day's picker opens at: the UI test checks only the month the server asks it to open at. Its first opening at the first day's month, and its keeping that month after a later change of the first day, were seen live on 2026-11-04.
- Remove in the absence list: no test; removing one absence with the usual confirmation was seen live on 2026-11-04.
- The "Dates required" mark of an empty first day in the absence editor: tested in English only; the Russian text («Заполните поле "Даты"») was seen live on 2026-11-05.
- A type deleted while the list is open: checked live on 2026-11-04 only. Its absences show the Deleted badge once the list is opened again; a reload of the browser page, which restores the open tab, keeps the rows as they were.
- Searching the drop-downs by typed text: the tests pick values without typing. The editor's user field, searched by username, first name, or last name, case-insensitively, with inactive users offered too, was checked live on 2026-11-04; the type field and the list's filter fields were not.
- The drop-downs' pickers and clear buttons: `AbsenceManagementRoleUiTest` opens and closes the editor's two pickers as a holder of Absence management; the list's filter pickers and every clear button have no test. All of them were seen in place on 2026-11-04, and none was used live.
- Pagination, and the filters after a reload of the browser page or after the browser's back or forward button: no test. The user, type, and period filters are tested, and were checked live on 2026-11-04.
- The refusal of a last day typed before the first, and the overlap refusal: tested in English only, except that the Russian text of the first is asserted in the bundle (`DateRangeFieldUiTest`). The first, with its new message, was seen live in English and Russian on 2026-11-05; the overlap refusal, and its clearing by a change of the user or of a date, on 2026-11-04, and in both languages and themes on 2026-11-05.
- The unsaved-changes question: tested on Esc and the close button, and checked live on 2026-11-04 on Esc and Cancel.
- The date range field on 2026-11-05, checked live in English and Russian, light and dark: the editor's one label, required mark, and error, with a new absence marked required at open; the period applying a picked date by itself, refusing a To before From without applying it, applying the fixed range, and surviving a reload of the list; the pickers' screen reader names. The UI tests check the pickers' names, the last day's minimum, the refusal's text in English, and the filter's overlap rule at both ends, each side empty, and an end before the start not applied; the stylesheet's stacking widths are checked by no test.
- What the live check saw and no test covers: a refusal on OK also shows the framework's validation toast; the end-before-start error takes the helper line's place, while a refusal on OK shows the range line with the error below it; the list shifts down while the period's error shows; the period's group has no accessible name of its own, only its pickers do.
