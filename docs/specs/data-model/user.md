# User

Source: `entity/user/User.java`, `security/DatabaseUserRepository.java`, `liquibase/changelog/010-init-user.xml`, `app/UserTimeZoneSupport.java`

A person who signs in to the application and logs time.

## Attributes

- **Username** — required; unique among non-deleted users (`docs/guidelines/database.md`).
- **Password** — stored encoded.
- **First name**, **last name**, **email**, **avatar**, **time zone** — optional; the email must be a valid address. The framework shows date-times to the user in their time zone, or in the browser's when none is set. Every screen counts today, and the time of day, in that same zone, so the screens agree with each other and with the user's own date whatever the server's zone. It is the zone of the user who is signed in, also when a screen shows someone else's records, such as another user's work schedule assignments or a time entry created for another user, and under user substitution.
- **Active** — on for a new user. An inactive user cannot sign in.
- Shown as `<first name> <last name> [<username>]`; screens show the full name and fall back to the username when both names are empty.

## Access

Roles are not stored on the user: they are Jmix role assignments, managed in the security screens ([security roles](../roles/README.md)). A user cannot sign in without a role that grants UI login, so a new user with no roles cannot sign in. The `admin` user, created by the schema changelogs in every environment, has [full access](../roles/system-full-access.md).

## Work schedule

A user's planned time comes from their [work schedule assignments](work-schedule-assignment.md), or from the default [work schedule](work-schedule.md#the-default-schedule) on the days none covers, except on the days of their [absences](absence.md), which plan nothing. The user entity itself has no schedule attribute; the user editor shows and edits the assignments ([users](../views/users.md#work-schedule)).

## Deletion

Deleting a user leaves their time entries and project participations in place and deletes their work schedule assignments and absences with them; restoring the user in Data Tools restores those assignments and absences.

## Not covered

- An inactive user being refused at sign-in: no test.
- Today in the browser's zone, for a user with no time zone set, and in the signed-in user's zone under user substitution: `UserTimeZoneSupportUiTest` checks only a user with a time zone of their own, on a clock pinned where the user's date is a day ahead of the server's, and no test or live check covers either.
