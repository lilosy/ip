package lily;

import java.io.IOException;

import lily.exception.LilyException;
import lily.parser.Parser;
import lily.storage.Storage;
import lily.task.Task;
import lily.task.TaskList;
import lily.ui.Ui;

/** Runs Lily's command-line task manager. */
public class Lily {
    private final Storage storage;
    private final TaskList tasks;
    private final Ui ui;

    /**
     * Sets up Lily against the given save-file path, loading any tasks already
     * saved
     * there.
     *
     * @param filePath path to the save file, e.g. {@code "data/lily.txt"}
     */
    public Lily(String filePath) {
        ui = new Ui();
        storage = new Storage(filePath);
        // load() never throws: a missing, unreadable, or partially corrupted save file
        // is
        // handled internally (with a printed warning) so startup always succeeds.
        tasks = new TaskList(storage.load());
    }

    /** Runs the read-command/act/respond loop until the user says {@code bye}. */
    public void run() {
        ui.showWelcome();

        while (true) {
            String userInput = ui.readCommand();
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
                        throw new LilyException(
                                "Please provide a task number to mark, e.g. \"mark 2\".");
                    }
                    taskListChanged = markTask(argument);
                } else if (command.equals("unmark")) {
                    if (argument.isEmpty()) {
                        throw new LilyException(
                                "Please provide a task number to unmark, e.g. \"unmark 2\".");
                    }
                    taskListChanged = unmarkTask(argument);
                } else if (command.equals("todo")) {
                    addTodo(userInput);
                    taskListChanged = true;
                } else if (command.equals("deadline")) {
                    addDeadline(userInput);
                    taskListChanged = true;
                } else if (command.equals("event")) {
                    addEvent(userInput);
                    taskListChanged = true;
                } else if (command.equals("delete")) {
                    if (argument.isEmpty()) {
                        throw new LilyException(
                                "Please provide a task number to delete, e.g. \"delete 2\".");
                    }
                    taskListChanged = deleteTask(argument);
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
                // Last-resort safety net: an unexpected bug in one command should never crash
                // the whole session or lose the in-memory task list.
                ui.showUnexpectedError(e);
                ui.showDivider();
            }
        }

        ui.showGoodbye();
    }

    /** Marks the requested task as done and reports whether it was found. */
    private boolean markTask(String taskNum) {
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
    private boolean unmarkTask(String taskNum) {
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

    private void addTodo(String userInput) throws LilyException {
        Task newTask = Parser.parseTodo(userInput);
        tasks.add(newTask);
        ui.showTaskAdded(newTask, tasks);
    }

    private void addDeadline(String userInput) throws LilyException {
        Task newTask = Parser.parseDeadline(userInput);
        tasks.add(newTask);
        ui.showTaskAdded(newTask, tasks);
    }

    private void addEvent(String userInput) throws LilyException {
        Task newTask = Parser.parseEvent(userInput);
        tasks.add(newTask);
        ui.showTaskAdded(newTask, tasks);
    }

    /** Removes the requested task and reports whether the task list changed. */
    private boolean deleteTask(String argument) {
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

    public static void main(String[] args) {
        new Lily("data/lily.txt").run();
    }
}
