# User screens

Source: `view/user/`, `component/avatarupload/`

The user list and the user editor, under the Administration menu. What a user is: [user](../data-model/user.md).

## Users

`users`. Lists users by username, with the user (avatar, full name or username, and an Active or Inactive badge), email, and time zone. It offers a filter on any attribute and pagination, both kept in the URL.

Actions: Create, Edit, Remove, and Role assignments; a gear menu holds Change password, Reset password, and User substitutions. These last four are the framework's security screens.

## User editor

`users/:id`, a 640px dialog when opened as one.

- **Avatar** — a round field: the image, or a person placeholder when there is none. While the avatar can be changed, the placeholder carries a plus and a pencil badge sits on the avatar; a click on the avatar, or Enter, opens a menu below it with **Upload image**, which opens the file picker, and, when there is an image, **Remove image**. A file dropped on the avatar is uploaded too, and the avatar shows a Drop hint while a file is dragged over it; of several dropped files, the first is uploaded and a warning says only one file can be uploaded. A ring spins on the avatar while a file uploads. Only PNG, JPEG, GIF, and WebP images of up to 2 MB are accepted, judged by the file's content, whatever its name or declared type. A file that is too large, is not a supported image, or fails to upload is refused with a warning notification saying why, and the image stays as it was; the field never shows an error. In a read-only editor, or for a user who may not change the avatar, the avatar is not a control: it shows no plus or badge and offers no upload or removal, by menu or by drop.
- **Username** — editable, and focused, only while the user is being created.
- **Password** and **confirm password** — shown only while the user is being created, then both required; the save is refused with "Passwords do not match" unless they are equal. An existing user's password is changed with the list's password actions.
- **First name**, **last name**, **email**.
- **Time zone** — a drop-down of every time zone ID.
- **Active** — a switch.

Saving a new user shows the warning "User has no assigned roles": roles are assigned separately, with Role assignments.

## Not covered

- The password mismatch, the locked username, and the warning: the user editor's UI tests cover only creating a user with matching passwords and the avatar field.
- The avatar field in a browser: the menu, the file picker, dropping files, the upload itself with the browser's own size and format checks, and how each state looks. The UI tests set, remove, and refuse images without sending a file, so only a live check shows these. Opening the file picker from the menu was checked live only in Chromium.
- The avatar becoming read-only for a user who may not change it: no test; the UI test covers only a read-only editor.
