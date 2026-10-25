# User

Source: `entity/user/User.java`, `security/DatabaseUserRepository.java`, `liquibase/changelog/010-init-user.xml`

A person who signs in to the application and logs time.

## Attributes

- **Username** — required; unique among non-deleted users (`docs/guidelines/database.md`).
- **Password** — stored encoded.
- **First name**, **last name**, **email**, **avatar**, **time zone** — optional; the email must be a valid address. The framework shows date-times to the user in their time zone, or in the browser's when none is set.
- **Active** — on for a new user. An inactive user cannot sign in.
- Shown as `<first name> <last name> [<username>]`; screens show the full name and fall back to the username when both names are empty.

## Access

Roles are not stored on the user: they are Jmix role assignments, managed in the security screens ([security roles](../roles/README.md)). A user cannot sign in without a role that grants UI login, so a new user with no roles cannot sign in. The `admin` user, created by the schema changelogs in every environment, has [full access](../roles/system-full-access.md).

## Work schedule

A user's planned time comes from their [work schedule assignments](work-schedule-assignment.md), or from the default [work schedule](work-schedule.md#the-default-schedule) on the days none covers. The user entity itself has no schedule attribute.

## Deletion

Deleting a user leaves their time entries and project participations in place and deletes their work schedule assignments with them; restoring the user in Data Tools restores those assignments.

## Not covered

- An inactive user being refused at sign-in: no test.
