# Lily User Guide

Lily is a chat-style task manager for todos, deadlines, and events.

## Viewing a daily schedule

Use `schedule <date>` to see deadlines due on a date and events whose date range
includes that date. A bare `schedule` command shows today, while `schedule today`
and `schedule tomorrow` select those relative dates explicitly.

Accepted date formats are `yyyy-MM-dd` and `d/M/yyyy`, including unpadded values
such as `2026-9-6`. Schedule dates do not accept a time.

Examples:

```text
schedule
schedule today
schedule tomorrow
schedule 2026-09-16
schedule 16/9/2026
```

Undated todos are excluded. Multi-day events appear on their start date, end date,
and every calendar date between them. Entries displayed without a time come first;
the remaining entries are ordered by their effective time on the selected day. Ties
are resolved by the task's number in the full list.

Incomplete and completed tasks are shown in separate sections, and the original
task numbers are retained so commands such as `mark 4` and `delete 2` can be used
directly:

```text
Here is your schedule for Sep 16 2026:
Not completed:
2. [D][ ] submit report (by: Sep 16 2026)
4. [E][ ] meeting (from: Sep 16 2026, 9:00AM to: Sep 16 2026, 10:00AM)
Completed:
7. [D][X] call client (by: Sep 16 2026, 2:00PM)
```

If the date has no scheduled tasks, Lily responds:

```text
There are no scheduled tasks for Sep 16 2026.
```

An invalid or impossible date is rejected:

```text
I couldn't understand the date '2026-02-30'. Try formats like: 2019-10-15 or 2/12/2019.
```

Viewing a schedule does not change the task list or the saved data.
