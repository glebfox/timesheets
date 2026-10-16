# User screens

Source: `view/user/`, `component/imageupload/`

The user list and the user editor, under the Administration menu. What a user is: [user](../data-model/user.md).

## Users

`users`. Lists users by username, with the user (avatar, full name or username, and an Active or Inactive badge), email, and time zone. It offers a filter on any attribute and pagination, both kept in the URL.

Actions: Create, Edit, Remove, and Role assignments; a gear menu holds Change password, Reset password, and User substitutions. These last four are the framework's security screens.

## User editor

`users/:id`, a 640px dialog when opened as one.

- **Avatar** — the image, or a placeholder when there is none, with upload and clear controls.
- **Username** — editable, and focused, only while the user is being created.
- **Password** and **confirm password** — shown only while the user is being created, then both required; the save is refused with "Passwords do not match" unless they are equal. An existing user's password is changed with the list's password actions.
- **First name**, **last name**, **email**.
- **Time zone** — a drop-down of every time zone ID.
- **Active** — a switch.

Saving a new user shows the warning "User has no assigned roles": roles are assigned separately, with Role assignments.

## Not covered

- The password mismatch, the locked username, and the warning: the UI test covers only creating a user with matching passwords.
