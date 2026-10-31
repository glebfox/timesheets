# Approve time entries

Source: `view/timeentry/approval/`, `app/TimeEntryApprovalSupport.java`, `app/util/ApprovalMode.java`, `app/util/ApprovalOverview.java`, `app/util/ApprovalPeriod.java`, `app/util/ApprovalPerson.java`, `app/util/ApprovalTotals.java`, `app/util/UnsavedInputSupport.java`, `menu.xml`

`approve-time-entries`, in menu Project Management after Time entries. The screen where the managers and approvers of projects review the [time entries](../data-model/time-entry.md) logged on them, person by person, in a week or a month, and approve or reject them. Who may open it: [Time entries: approval](../roles/time-entry-approval.md). Which entries a user sees on it is decided by their project roles, not by the security role. The screen is laid out for a desktop width and has no layout for a phone.

## Which entries it shows

A user approves on a project where they are a [participant](../data-model/project-participant.md) whose [project role](../data-model/project-role.md) is of type Manager or Approver. Member and Observer grant nothing, and a user who is a Manager on one project and a Member on another sees only the first project's entries.

- A **Manager** approves every entry logged on the project's tasks: members', approvers', other managers', and their own.
- An **Approver** approves every entry on the project's tasks except their own and those of the project's Managers, the users who are participants of that project with a Manager-type role. Another Approver's entries are included. An entry of a user who is not a participant of the project, any more or ever, counts as a member's.
- The same rule decides what the screen lists and what the approve and reject operations accept: an entry outside it is refused, whoever calls them.
- The approver is the effective user, so under user substitution it is the substituted user.
- The projects offered are those where the user is a Manager or an Approver, open and closed. Entries of closed projects and of inactive tasks are shown, as are entries of every status, with Approved ones dimmed and Closed ones without buttons.
- A deleted participant, project role, task, or project grants nothing and has no entries to approve. A participant whose project role was deleted grants nothing and does not count as a Manager, so an Approver may approve that person's entries, while the roster still shows the role's name. The entries of a deleted user are listed, and their participations still count, so a deleted Manager's entries stay off an Approver's list. The entries of a deleted task are not listed.

A user who approves nowhere, Full access included, sees an empty screen that says so, without the roster or the project choice.

## Toolbar

- **Week | Month** — a switch, Month by default. The mode is remembered per user, and saved only once a change is made.
- **Period** — arrows to the previous and the next period around the period's name, such as "October 2026" or "Oct 18 – 24 2026", and a button back to the default period: "This month", the current month, or "Last week", the week before the current one. The button is disabled while the default period shows. Weeks start as in the [calendar](calendar.md): on Sunday in English, on Monday in Russian.
- **Project** — "All my projects" or one of the projects the user approves on, sorted by name; it can be cleared.
- **Team progress** — "N of M people fully approved", the number of entries waiting and rejected, and a bar split by status. A person is fully approved when they have entries and none is New or Rejected. It counts everyone with entries in the period and the chosen project, whatever the roster's switch and search show, and is hidden when nobody has entries, keeping its place.

On a narrower desktop, such as 1280 pixels wide with the navigation drawer open, the team progress always has a row of its own below the other parts of the toolbar. Which of the two layouts shows depends on the width of the screen alone, so changing the mode, the period, or the language never moves the roster.

"Waiting" means status New throughout the screen.

## Roster

On the left, a card for everyone with entries in the period, showing:

- the avatar, the name (the first and last name, or the username), and the names of the person's project roles on the projects of their entries, none for a person who is not a participant of them;
- a badge: "N waiting", with a red bare count of the rejected entries beside it when there are any; "N rejected" alone when nothing waits; "✓ Approved" when every entry is Approved or Closed;
- a fingerprint of the period, one bar per day, as tall as the time logged that day, full at 10 hours, in the color of the day's status: Rejected when any entry of the day is, otherwise New when any is, otherwise Approved when any is, otherwise neutral for Closed;
- the time logged in the period.

A search field finds a person by a part of the first name, the last name, or the username, whatever the case; a full name typed across both names matches nobody. A switch shows **Needs review**, the people with waiting entries and the default, or **Everyone**. People with waiting entries come first, then by name ignoring case.

The first person shown is selected when the screen opens and when the mode, the period, or the project changes. After an action the selected person stays while still shown. A card is chosen by a click, or by Enter or Space; choosing the selected person again changes nothing. When the switch or the search hides the selected person, the first person shown is selected.

With nobody to show, a message says why: the user approves on no project, nobody logged time in the period (naming the project when one is chosen), nobody has time waiting (under Needs review), or nobody matches the search.

## The selected person

A header with the avatar, the name, the roles, and the projects of their entries in the period, and the primary button that approves their waiting entries: "Approve N waiting" in a month, "Approve week · N" in a week. Below it, four figures that are always there: Logged, with its time, its number of entries, and a bar split by status, and Waiting, Approved, and Rejected, each with its entries and time, muted at zero.

**Month.** A grid in the layout of the calendar, one row per week, Saturday and Sunday shaded. A day shows its number and, with entries, its time with a dot and a line in the color of the day's status. The days of the neighboring months are dimmed, show nothing, and count nowhere: the week cards, the figures, the fingerprint, and the list count only the month's own days. Beside each row, a week card shows the time, the number of entries, and the status badge, with a **Week** button below them that approves the week's waiting entries while it has any. Below the grid, the entries of one week, headed by the week's dates and its status badges and grouped by day, each day headed such as "Mon, Oct 19 · 6h" with an **Approve day** button while it has waiting entries; a week without entries says "No time logged on these days." A click on a week card selects that week, and a click on a day selects its week and scrolls to the day's entries. The week listed when a person opens is the first one with waiting entries, and with none, the last week that has their entries. A reload keeps the week while the same person stays selected.

**Week.** A board of seven columns in the order of the user's week, each headed by the day and its time, with **Approve day** while the day has waiting entries, and the day's entries as compact cards whose description is cut after three lines. A column too narrow for the text of Approve day shows its ✓ alone, which names itself on hover; a card's buttons go below its text unless the column is wide enough for them beside the time; and where seven columns wide enough for their cards do not fit, the board scrolls sideways within itself.

**An entry** shows a stripe in the color of its status, the time, the task with its project, a badge of its status unless it is New, the description and, for a Rejected entry, the rejection reason, both rendered from Markdown, and the tags as badges, each naming its group on hover. Its buttons: ✓ for a New or a Rejected entry, ✕ for a New, an Approved, or a Rejected one, none for a Closed one.

## Approving

- **Entry** — ✓ sets a New or Rejected entry to Approved at once, with no question. A rejected entry keeps its rejection reason.
- **Day, week, person** — Approve day, Week, and the header's button each approve the person's New entries dated in that stretch: one day, the week's days of the month, or the whole period. A button is there only while its stretch has such entries, and the person's button names their number. A rejected entry is not part of it, as it takes its own ✓, and neither is an entry in the reject basket, which the counts leave out as well. The button asks first, naming the count, the person, the days, and the time, such as "Approve 5 entries of Cid Cole for October 2026, 20h?"; No leaves everything as it was.

After an approval the screen is loaded again, the person and the week stay, and a notification gives the number approved. Nothing records who approved or rejected an entry, or when: only the status and the rejection reason change.

An approval is all or nothing. When any entry was changed, deleted, or moved out of the approver's reach after the screen loaded it, or its task or project was deleted, nothing is approved: the question closes, the screen is loaded again with the entries as they are now, and the framework shows its message that the entry changed. The operations never approve what the approver has not seen. An entry that is outside the rule, Closed, or already Approved is refused by the operation, though the screen cannot send one except through such a change.

Approving and rejecting tell the entry's user as any change of status does ([time entry](../data-model/time-entry.md#status)).

## Rejecting

Rejecting goes through the reject basket, which holds entries of the selected person.

- ✕ puts an entry in the basket. The entry is marked "In reject basket", and its buttons give way to ↺, which takes it out again. Nothing is saved yet.
- While the basket has entries, a bar floats over the bottom of the person's part with their number, their dates, with a run of days written as a range such as "Oct 5 – 7, Oct 14", **Clear**, which empties the basket, and **Write a reason**.
- **Write a reason** opens a modal side dialog on the right, which the screen behind it does not react to. It lists the basket's entries by day, each with a button that takes it out; four quick reasons, Add a description, Wrong task, Hours look too high, and Duplicate entry, each of which appends its sentence to the reason on a line of its own; the reason, required, a Markdown editor, where blanks count as no reason; **Reject N entries**; and Cancel.
- Reject sets every entry of the basket to Rejected with the reason, replacing an earlier one, and empties the basket. The screen is loaded again, and a notification gives the number rejected. It is all or nothing, as an approval is: an entry that changed meanwhile fails the whole rejection, the dialog closes, the screen is loaded again, and the basket keeps its entries as they are now, and Write a reason opens the dialog again on the reason written.
- Cancel, Esc, and a click outside the dialog close it, asking the usual unsaved-changes question first while the reason differs from how the dialog opened; Yes closes it and the basket stays. Taking the last entry out of the basket closes the dialog without asking.

A change that would leave the basket behind asks "Discard N entries from the reject basket?" first: choosing another person, the arrows or the default-period button of the period, the mode, or the project. Yes empties the basket and makes the change. No keeps everything as it was, with the field put back and the mode not remembered. The roster's switch and search ask the same only when they would hide the selected person. A reload that hides the selected person, such as approving their last waiting entries under Needs review while the basket holds only Approved or Rejected entries, drops the basket and the reason without asking. Every reload keeps in the basket the entries the selected person still has in the period that can still be rejected, as they are now; an entry that is gone, moved out of the period, or Closed leaves it.

## Not covered

- How the screen looks, which no test sees (`docs/guidelines/testing.md`): the status colors, stripes, dots, and lines, the dimming of Approved entries, the three-line cut on the week board, the place of the floating bar and the side dialog and its width, the focus after a dialog closes, and the smooth scroll to a day.
- The roster as users see it: its cards come from a renderer that never runs in a UI test, so the tests build a card directly. What the view gives a card, the role names and the selection mark, and the card's redraw in place when another person is chosen are checked by no test.
- The reject basket's edges: the questions of the period's previous and default-period buttons and of the roster's search, and a basket in Week mode or across two weeks. The tests ask through Next, the mode, the project, the roster's switch, and the view's own `selectPerson`. A reload that hides the selected person, and the entries a reload takes out of the basket (above), are untested too.
- Choosing a person by a click, Enter, or Space on a roster card: the cards never exist in a UI test, so the tests choose through the view's own `selectPerson`.
- Keyboard focus after ✕ and ↺: the clicked button leaves the card, so the focus is lost.
- An entry that left the approver's reach without changing, such as when their participation was removed meanwhile: the operation refuses it, as its tests show, but the screen does not catch the refusal, and no UI test shows what a user then sees. A participation removed between the operation's check and its save is not noticed, though the entry's own version is still guarded.
- The framework's message for an entry that changed meanwhile: the UI tests see only the exception.
- The screen under user substitution: the tests check the effective user in the support bean only.
- A project without a Manager and with a single Approver leaves that Approver's own entries to nobody on this screen: only someone with the Time entries list can change them by hand. Whether an Approver is restricted further than not approving their own and the Managers' entries is undecided. No test pins it.
- Entries of closed projects and of inactive tasks being listed: the tests check only that a closed project is offered in the project choice.
- The tags on an entry's card, each naming its group on hover: no UI test checks them; the support tests check only that the tags are loaded.
- The empty-state messages other than two: the tests check only that of a user who approves on no project and that of a period with no entries. A chosen project with no time, nobody with time waiting under Needs review, and a search that matches nobody are untested, as is the team progress hiding itself when nobody has entries.
- The entries of a deleted task are listed nowhere on this screen, so nobody approves them here; the test checks only that they are absent.
- The author's rejected badge refreshing after a rejection: it needs a second live session; the tests check that the operations tell the entry's user.
