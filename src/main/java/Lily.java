import java.io.IOException;

/** Runs Lily's command-line task manager. */
public class Lily {

    public static void main(String[] args) {
        Ui ui = new Ui();
        Storage storage = new Storage("data/lily.txt");
        // load() never throws: a missing, unreadable, or partially corrupted save file is
        // handled internally (with a printed warning) so startup always succeeds.
        TaskList tasks = new TaskList(storage.load());

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
            String command = Parser.getCommandWord(userInput);
            String argument = Parser.getArguments(userInput);
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
                    taskListChanged = deleteTask(ui, tasks, argument);
                } else {
                    ui.showInvalidCommand();
                }
                if (taskListChanged) {
                    storage.save(tasks.toList());
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
        int taskIndex;
        try {
            taskIndex = Parser.parseTaskIndex(taskNum);
        } catch (LilyException e) {
            ui.showInvalidTaskNumber();
            return false;
        }
        if (!tasks.containsIndex(taskIndex)) {
            ui.showMissingTask();
            return false;
        }
        tasks.mark(taskIndex);
        Task task = tasks.get(taskIndex);
        ui.showTaskMarked(task);
        return true;
    }

    /** Marks the requested task as not done and reports whether it was found. */
    public static boolean unmarkTask(Ui ui, TaskList tasks, String taskNum) {
        int taskIndex;
        try {
            taskIndex = Parser.parseTaskIndex(taskNum);
        } catch (LilyException e) {
            ui.showInvalidTaskNumber();
            return false;
        }
        if (!tasks.containsIndex(taskIndex)) {
            ui.showMissingTask();
            return false;
        }
        tasks.unmark(taskIndex);
        Task task = tasks.get(taskIndex);
        ui.showTaskUnmarked(task);
        return true;
    }

    public static void addTodo(Ui ui, TaskList tasks, String userInput) throws LilyException {
        Task newTask = Parser.parseTodo(userInput);
        tasks.add(newTask);
        ui.showTaskAdded(newTask, tasks);
    }

    public static void addDeadline(Ui ui, TaskList tasks, String userInput) throws LilyException {
        Task newTask = Parser.parseDeadline(userInput);
        tasks.add(newTask);
        ui.showTaskAdded(newTask, tasks);
    }

    public static void addEvent(Ui ui, TaskList tasks, String userInput) throws LilyException {
        Task newTask = Parser.parseEvent(userInput);
        tasks.add(newTask);
        ui.showTaskAdded(newTask, tasks);
    }

    /** Removes the requested task and reports whether the task list changed. */
    public static boolean deleteTask(Ui ui, TaskList tasks, String argument) {
        int taskIndex;
        try {
            taskIndex = Parser.parseTaskIndex(argument);
        } catch (LilyException e) {
            ui.showInvalidTaskNumber();
            return false;
        }
        if (!tasks.containsIndex(taskIndex)) {
            ui.showMissingTask();
            return false;
        }
        Task task = tasks.get(taskIndex);
        tasks.remove(taskIndex);
        ui.showTaskDeleted(task, tasks);
        return true;
    }

}
