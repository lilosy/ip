# Lily User Guide

Lily is a chat-style task manager for keeping track of todos, deadlines, and
events. Type a command in the input box and press **Enter** or click **Send**.
Lily replies in the chat and saves changes automatically.

## Starting Lily

1. Download or locate `lily.jar`.
1. Double-click `lily.jar` to open Lily.

If double-clicking does not open the app, make sure Java 25 or later is installed,
then open a terminal in the folder containing the JAR and run:

```text
java -jar lily.jar
```

On its first start, Lily creates a `data` folder next to where it is run and
stores your tasks in `data/lily.txt`. Keep this file if you want to keep your
tasks; Lily loads it automatically the next time it starts.

## Quick start

Try these commands in order:

```text
todo read chapter 1
deadline submit assignment /by 2026-09-25 2359
event study session /from 2026-09-20 1400 /to 2026-09-20 1600
list
```

Task numbers shown by `list` are used by commands such as `mark`, `unmark`, and
`delete`.

## Command reference

Commands are case-insensitive (`TODO` and `todo` work alike), but task
descriptions keep the spelling you type. Use a single space between command parts.

### Add a todo

```text
todo <description>
```

Example:

```text
todo buy groceries
```

### Add a deadline

```text
deadline <description> /by <date/time>
```

Examples:

```text
deadline return library book /by 2026-10-01
deadline submit report /by 1/10/2026 1800
```

### Add an event

```text
event <description> /from <start date/time> /to <end date/time>
```

Examples:

```text
event team meeting /from 2026-10-02 0930 /to 2026-10-02 1030
event holiday /from 2/10/2026 /to 5/10/2026
```

The end of an event cannot be before its start.

### List all tasks

```text
list
```

Each task has a type marker and status:

- `[ ]` means not completed; `[X]` means completed.
- `[D]` identifies a deadline and `[E]` identifies an event.
- A todo has no extra type marker.

### Mark or unmark a task

```text
mark <task number>
unmark <task number>
```

Examples:

```text
mark 2
unmark 2
```

Task numbers are positive whole numbers from `list` or `schedule`.

### Delete a task

```text
delete <task number>
```

Example:

```text
delete 3
```

Deleting is permanent from Lily's list, so check the task number before sending
the command.

### Find tasks

```text
find <keyword>
```

Example:

```text
find report
```

Search matches part of a task description and ignores letter case. `find` accepts
one keyword at a time.

### View a daily schedule

```text
schedule [date]
```

Examples:

```text
schedule
schedule today
schedule tomorrow
schedule 2026-09-16
schedule 16/9/2026
```

With no date, Lily shows today's schedule. The schedule includes deadlines due on
that date and events that span that date; todos are not included. Multi-day
events appear on every day from their start through their end. Incomplete and
completed tasks are shown separately, and the original task numbers are retained.

Entries without a specified time appear first, followed by timed entries in time
order. Viewing a schedule does not change your saved tasks.

## Dates and times

For deadlines and events, use either of these formats:

```text
yyyy-MM-dd
yyyy-MM-dd HHmm
d/M/yyyy
d/M/yyyy HHmm
```

For example, `2026-09-16 1800` means 6:00 PM on 16 September 2026. If you omit
the time, Lily treats the task as date-only and displays no time. Schedule dates
must not include a time.

Lily rejects impossible dates and times, such as `2026-02-30` or `2500`.

## Saving and closing

Lily saves after you add, mark, unmark, or delete a task. You can close the
window normally, or type the following to say goodbye:

```text
bye
```

You do not need to run a separate save command.

## If something goes wrong

Lily explains invalid commands in the chat. Check the command format, task number,
and date format, then try again. If Lily cannot read part of a damaged save file,
it skips only the invalid entries and lets you know at startup; valid tasks still
load.
