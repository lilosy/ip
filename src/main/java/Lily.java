import java.io.IOException;
import java.time.LocalDateTime;

/** Runs Lily's command-line task manager. */
public class Lily {

    public static void main(String[] args) {
        Ui ui = new Ui();
        // loadTasks() never throws: a missing, unreadable, or partially corrupted save
        // file is handled internally (with a printed warning) so startup always succeeds.
        TaskList tasks = new TaskList(Storage.loadTasks());

        ui.showWelcome();

        while (true) {
            String userInput;
            userInput = ui.readCommand();
            if (userInput == null) {
                break;
            }
            if (userInput.isEmpty()) {
                // Blank line: nothing to do, just prompt again instead of treating
                // it as an "invalid command" (split(" ", 2) on "" gives [""]).
                ui.showDivider();
                continue;
            }
            if (userInput.equals("bye")) {
                break;
            }
            String parts[] = userInput.split(" ", 2);
            String command = parts[0];
            String argument = parts.length > 1 ? parts[1].trim() : "";
            try {
                boolean taskListChanged = false;
                if (command.equals("list")) {
                    ui.showTaskList(tasks);
                } else if (command.equals("mark")) {
                    if (argument.isEmpty()) {
                        throw new LilyException("Please provide a task number to mark, e.g. \"mark 2\".");
                    }
                    taskListChanged = markTask(ui, tasks, argument);
                } else if (command.equals("unmark")) {
                    if (argument.isEmpty()) {
                        throw new LilyException("Please provide a task number to unmark, e.g. \"unmark 2\".");
                    }
                    taskListChanged = unmarkTask(ui, tasks, argument);
                } else if (command.equals("todo")) {
                    addTodo(ui, tasks, userInput);
                    taskListChanged = true;
                } else if (command.equals("deadline")) {
                    addDeadline(ui, tasks, userInput);
                    taskListChanged = true;
                } else if (command.equals("event")) {
                    addEvent(ui, tasks, userInput);
                    taskListChanged = true;
                } else if (command.equals("delete")) {
                    if (argument.isEmpty()) {
                        throw new LilyException("Please provide a task number to delete, e.g. \"delete 2\".");
                    }
                    taskListChanged = deleteTask(ui, tasks, userInput);
                } else {
                    ui.showInvalidCommand();
                }
                if (taskListChanged) {
                    Storage.saveTasks(tasks.toList());
                }
                ui.showDivider();
            } catch (LilyException | IOException e) {
                ui.showError(e);
                ui.showDivider();
            } catch (RuntimeException e) {
                // Last-resort safety net: an unexpected bug in one command should
                // never crash the whole session or lose the in-memory task list.
                ui.showUnexpectedError(e);
                ui.showDivider();
            }

        }

        ui.showGoodbye();
    }

    /** Marks the requested task as done and reports whether it was found. */
    public static boolean markTask(Ui ui, TaskList tasks, String taskNum) {
        try {
            int taskIndex = Integer.parseInt(taskNum) - 1;
            if (!tasks.containsIndex(taskIndex)) {
                ui.showMissingTask();
                return false;
            } else {
                tasks.mark(taskIndex);
                Task task = tasks.get(taskIndex);
                ui.showTaskMarked(task);
                return true;
            }

        } catch (NumberFormatException e) {
            ui.showInvalidTaskNumber();
            return false;
        }
    }

    /** Marks the requested task as not done and reports whether it was found. */
    public static boolean unmarkTask(Ui ui, TaskList tasks, String taskNum) {
        try {
            int taskIndex = Integer.parseInt(taskNum) - 1;
            if (!tasks.containsIndex(taskIndex)) {
                ui.showMissingTask();
                return false;
            } else {
                tasks.unmark(taskIndex);
                Task task = tasks.get(taskIndex);
                ui.showTaskUnmarked(task);
                return true;
            }

        } catch (NumberFormatException e) {
            ui.showInvalidTaskNumber();
            return false;
        }
    }

    public static void addTodo(Ui ui, TaskList tasks, String userInput) throws LilyException {
        String[] parts = userInput.split(" ", 2);
        if (parts.length < 2) {
            throw new LilyException("Add a description for the todo task");
        }
        Task newTask = new ToDo(parts[1]);
        tasks.add(newTask);
        ui.showTaskAdded(newTask, tasks);
    }

    public static void addDeadline(Ui ui, TaskList tasks, String userInput) throws LilyException{
        String[] parts = userInput.split(" ", 2);
        if (parts.length < 2) {
            throw new LilyException("Add a description for the deadline task");
        }
        String deadlineTaskdDesc = parts[1];
        String[] deadlineParts = deadlineTaskdDesc.split(" /by ", 2);
        if (deadlineParts.length < 2) {
            throw new LilyException("Add a deadline for the task");
        }
        String description = deadlineParts[0].trim();
        if (description.isEmpty()) {
            throw new LilyException("Add a description for the deadline task");
        }
        LocalDateTime by = DateTimeParser.parseUserInput(deadlineParts[1]);
        Task newTask = new Deadline(description, by);
        tasks.add(newTask);
        ui.showTaskAdded(newTask, tasks);
    }

    public static void addEvent(Ui ui, TaskList tasks, String userInput) throws LilyException {
        String[] parts = userInput.split(" ", 2);
        if (parts.length < 2) {
            throw new LilyException("Add a description for the event");
        }
        if(!parts[1].matches(".*\\s/from\\s.*\\s/to\\s.*")) {
            throw new LilyException("Wrong format for event, use this format: event [event desc] /from [...] /to [...]");
        }
        String eventTaskDesc = parts[1];
        String[] eventParts = eventTaskDesc.split(" /from | /to ", 3);
        String description = eventParts[0].trim();
        if (description.isEmpty()) {
            throw new LilyException("Add a description for the event");
        }
        LocalDateTime from = DateTimeParser.parseUserInput(eventParts[1]);
        LocalDateTime to = DateTimeParser.parseUserInput(eventParts[2]);
        if (to.isBefore(from)) {
            throw new LilyException("The event's 'to' time can't be before its 'from' time.");
        }
        Task newTask = new Event(description, from, to);
        tasks.add(newTask);
        ui.showTaskAdded(newTask, tasks);
    }

    /** Removes the requested task and reports whether the task list changed. */
    public static boolean deleteTask(Ui ui, TaskList tasks, String userInput) throws LilyException{
        String parts[] = userInput.split(" ", 2);
        if (parts.length != 2) {
            throw new LilyException("Wrong format for delete, please enter as such: delete taskNumber");
        }
        try {
            int taskIndex = Integer.parseInt(parts[1]) - 1;
            if (!tasks.containsIndex(taskIndex)) {
                ui.showMissingTask();
                return false;
            } else {
                Task task = tasks.get(taskIndex);
                tasks.remove(taskIndex);
                ui.showTaskDeleted(task, tasks);
                return true;
            }
        } catch (NumberFormatException e) {
            ui.showInvalidTaskNumber();
            return false;
        }

    }

}
