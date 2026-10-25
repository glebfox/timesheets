# Data model

Source: `entity/`, `datatype/`

The entities at a glance; each has its own spec in this directory.

```mermaid
classDiagram
    direction TB

    class Client {
        <<Entity>>
        String name
        byte[] image
    }
    class ContactInformation {
        <<Embeddable>>
        String email
        String phone
        String address
        String url
    }
    class Project {
        <<Entity>>
        String name
        String description
        ProjectStatus status
    }
    class Task {
        <<Entity>>
        String name
        String description
        TaskStatus status
    }
    class TaskType {
        <<Entity>>
        String name
        String description
    }
    class ProjectParticipant {
        <<Entity>>
    }
    class ProjectRole {
        <<Entity>>
        String name
        ProjectRoleType type
        String description
    }
    class TagGroup {
        <<Entity>>
        String name
    }
    class Tag {
        <<Entity>>
        String name
    }
    class TimeEntry {
        <<Entity>>
        LocalDate date
        WorkTime spentTime
        TimeEntryStatus status
        String description
        String rejectionReason
    }
    class WorkSchedule {
        <<Entity>>
        String name
        String description
        WorkTime workdayTime
        Boolean monday … sunday
        Boolean isDefault
    }
    class WorkScheduleAssignment {
        <<Entity>>
        LocalDate startDate
    }
    class HolidayCalendar {
        <<Entity>>
        String name
        String description
    }
    class HolidayCalendarDay {
        <<Entity>>
        LocalDate date
        HolidayCalendarDayType type
        String name
    }
    class User {
        <<Entity>>
        String username
        String password
        String firstName
        String lastName
        String email
        Boolean active
        String timeZoneId
        byte[] avatar
    }

    Client *-- ContactInformation : contactInformation
    Project "*" --> "1" Client : client
    Project "1" *-- "*" Task : tasks
    Project "1" *-- "*" ProjectParticipant : participants
    Project "1" *-- "*" TagGroup : tagGroups
    TagGroup "1" *-- "*" Tag : tags
    Task "*" --> "1" TaskType : type
    Task "*" --> "*" TagGroup : requiredTagGroups
    ProjectParticipant "*" --> "1" User : user
    ProjectParticipant "*" --> "1" ProjectRole : role
    TimeEntry "*" --> "1" Task : task
    TimeEntry "*" --> "1" User : user
    TimeEntry "*" --> "*" Tag : tags
    WorkScheduleAssignment "*" --> "1" User : user
    WorkScheduleAssignment "*" --> "1" WorkSchedule : workSchedule
    WorkSchedule "*" --> "0..1" HolidayCalendar : holidayCalendar
    HolidayCalendar "1" *-- "*" HolidayCalendarDay : days
```

A filled diamond marks an owner and its parts. Every entity also has a UUID `id` and a `version` for optimistic locking, and every entity so far has the soft-delete attributes `deletedBy` and `deletedDate`; the diagram leaves them out.

- A [project](project.md) owns its [tasks](task.md), [participants](project-participant.md), and [tag groups](tag-group.md), and a tag group owns its tags: they are edited inside their owner and deleted with it.
- A [time entry](time-entry.md) is one user's time on one task and may carry tags; the editor offers those of the task's project. Its duration is a [work time](work-time.md) value.
- A [work schedule](work-schedule.md) plans how long each workday is and which days of the week are workdays; a [holiday calendar](holiday-calendar.md) it uses overrides the week on dated days. [Assignments](work-schedule-assignment.md) tie a user to a schedule from a date; exactly one schedule is the default, for the days no assignment covers. A user's plan for a day comes from them.
- A holiday calendar owns its days and edits them inside itself, but a deleted calendar keeps them, so the schedules that still use it plan as before.
- [Clients](client.md), [task types](task-type.md), [project roles](project-role.md), [users](user.md), work schedules, and holiday calendars stand on their own: deleting one leaves whatever refers to it in place, except that a user's work schedule assignments are deleted with the user.

## Rules that hold for every entity

- Every entity so far is soft-deleted: deleting marks a row instead of removing it, and uniqueness holds among non-deleted rows only (`docs/guidelines/database.md`). Soft deletion is chosen per entity, not required of every one (`docs/conventions.md`).
- An entity enforces its required attributes, its value ranges, and database uniqueness on every save, and deletes what its deletion takes with it, such as a project's tasks or a user's work schedule assignments, on every delete. Each spec says which of its other rules also hold on every save, such as the one default work schedule, and which are applied only by the editors, such as which tasks are bookable or when a rejection reason is required; a save through `DataManager` bypasses the latter.
