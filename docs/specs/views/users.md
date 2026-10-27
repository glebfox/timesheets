# User screens

Source: `view/user/`, `component/avatarupload/`, `action/WorkScheduleAssignmentCreateAction.java`, `action/WorkScheduleAssignmentEditAction.java`, `action/WorkScheduleAssignmentRemoveAction.java`, `app/WorkScheduleAssignmentSupport.java`

The user list and the user editor, under the Administration menu. What a user is: [user](../data-model/user.md).

## Users

`users`. Lists users by username, with the user (avatar, full name or username, and an Active or Inactive badge), email, and time zone. It offers a filter on any attribute and pagination, both kept in the URL.

Actions: Create, Edit, Remove, and Role assignments; a gear menu holds Change password, Reset password, and User substitutions. These last four are the framework's security screens. Each of them is hidden from a user who may not open its screen. Change password and Reset password are hidden as well from a user who may not edit users, whatever screens the user may open ([decision 0003](../../decisions/0003-work-schedule-management-grants-no-update-on-users.md)). The gear menu is hidden when none of its items is left.

## User editor

`users/:id`, a 640px dialog when opened as one.

- **Avatar** — a round field: the image, or a person placeholder when there is none. While the avatar can be changed, the placeholder carries a plus and a pencil badge sits on the avatar; a click on the avatar, or Enter, opens a menu below it with **Upload**, which opens the file picker, and, when there is an image, **Remove**. A file dropped on the avatar is uploaded too, and the avatar shows a Drop hint while a file is dragged over it; of several dropped files, the first is uploaded and a warning says only one file can be uploaded. A ring spins on the avatar while a file uploads. Only PNG, JPEG, GIF, and WebP images of up to 2 MB are accepted, judged by the file's content, whatever its name or declared type. A file that is too large, is not a supported image, or fails to upload is refused with a warning notification saying why, and the image stays as it was; the field never shows an error. In a read-only editor, or for a user who may not change the avatar, the avatar is not a control: it shows no plus or badge, offers no upload or removal, by menu or by drop, and is not dimmed.
- **Username** — editable, and focused, only while the user is being created.
- **Password** and **confirm password** — shown only while the user is being created, then both required; the save is refused with "Passwords do not match" unless they are equal. An existing user's password is changed with the list's password actions.
- **First name**, **last name**, **email**.
- **Time zone** — a drop-down of every time zone ID.
- **Active** — a switch.

Saving a new user shows the warning "User has no assigned roles": roles are assigned separately, with Role assignments.

### Work schedule

A section under the fields with the user's [work schedule assignments](../data-model/work-schedule-assignment.md): the start date and the schedule, newest first as the editor opens. An assignment created here goes to the top, whatever its date, and an edited one stays where it is. Why it is not sorted into place: [decision 0004](../../decisions/0004-editor-tables-keep-their-load-order.md). The columns fit their content, and fit again when an assignment is created or edited. A green Current badge marks the assignment in force today: the latest start on or before today, as the assignments stand in the editor. It moves at once when an assignment is created, edited, or removed. Future assignments get no badge. An assignment whose schedule is deleted stays, with the schedule's name and a neutral Deleted badge. An empty table reads "No assignments"; nothing names the default schedule that such a user works to. For a user who may not read assignments, the table stays empty and reads "No assignments", whatever assignments the user has.

Multiple selection, with Create, Edit, and Remove. Create and Edit open the [assignment editor](settings.md#assignment-editor) without the user field; Remove asks the usual confirmation and removes every selected assignment. Their changes reach the database only when the user is saved, a new user included: OK saves the user and the assignment changes together, and Cancel discards both.

The section stays editable when the editor opens read-only for a user who may not edit users but may change assignments, as a holder of [work schedule management](../roles/work-schedule-management.md) does: the user's fields cannot change, and OK saves only the assignment changes.

## Not covered

- The password mismatch, the locked username, and the warning: the user editor's UI tests cover only creating a user with matching passwords, the avatar field, and the Work schedule section.
- Hiding the security actions: the UI tests cover `admin`, who sees all four, and a user who may not edit users and may open none of their screens but Change password's, who sees none; no other combination.
- How the Work schedule section looks in the editor, with its badges and its empty table, and Cancel closing an unchanged editor that has assignments without asking to save: checked live only.
- In a browser, the Work schedule columns fitting their content after an assignment is created or edited, and the Current badge moving to another row after an edit: the UI tests check only that the browser is asked to measure the columns again and that an edit redraws every row. The Current badge moving was checked live.
- The avatar field in a browser: the menu, the file picker, dropping files, the upload itself with the browser's own size and format checks, and how each state looks. The UI tests set, remove, and refuse images without sending a file, so only a live check shows these. Opening the file picker from the menu was checked live only in Chromium.
- The avatar becoming read-only for a user who may not change it: no test; the UI test covers only a read-only editor.
