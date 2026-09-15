package lily;

import java.io.IOException;

import java.util.List;

import lily.exception.LilyException;
import lily.parser.Parser;
import lily.storage.Storage;
import lily.task.Task;
import lily.task.TaskList;
import lily.ui.Ui;

/** Runs Lily's command-line task manager. */
public class Lily {
    /** Greeting shared by both the command-line and graphical interfaces. */
    public static final String WELCOME_MESSAGE = "Hey there! I'm Lily.\nWhat would you like to do today?";
    /** Farewell shared by both the command-line and graphical interfaces. */
    public static final String GOODBYE_MESSAGE = "Bye! See you soon :)";
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

    /**
     * Processes one command and returns Lily's reply. Both the CLI and the GUI
     * display this exact reply, so command behaviour is defined in one place.
     *
     * @param input command typed by the user
     * @return a human-readable reply
     */
    public String getResponse(String input) {
        String userInput = input == null ? "" : input.trim();
        if (userInput.isEmpty()) {
            return "Please enter a command.";
        }
        if (userInput.equals("bye")) {
            return "Bye! See you soon :)";
        }

        try {
            CommandResult result = executeCommand(userInput);
            if (result.changedTaskList()) {
                storage.save(tasks.toList());
            }
            return result.response();
        } catch (LilyException | IOException e) {
            return e.getMessage();
        } catch (RuntimeException e) {
            return "Something went wrong handling that command: " + e.getMessage();
        }
    }

    /** Executes a parsed command without performing persistence. */
    private CommandResult executeCommand(String userInput) throws LilyException {
        String command = Parser.getCommandWord(userInput);
        String argument = Parser.getArguments(userInput);
        switch (command) {
        case "list":
            return unchanged(formatTaskList(tasks.toList(), "Here are the tasks in your list:"));
        case "mark":
            return changeTaskStatus(argument, true);
        case "unmark":
            return changeTaskStatus(argument, false);
        case "todo":
        case "deadline":
        case "event":
            return addTask(command, userInput);
        case "delete":
            return deleteTask(argument);
        case "find":
            return findTasks(argument);
        default:
            return unchanged("I don't recognise that command.");
        }
    }

    private CommandResult changeTaskStatus(String argument, boolean shouldMarkDone) throws LilyException {
        String command = shouldMarkDone ? "mark" : "unmark";
        int index = parseRequiredTaskIndex(argument, command);
        if (!tasks.containsIndex(index)) {
            return unchanged("That task number does not exist.");
        }

        Task task = shouldMarkDone ? markTask(index) : unmarkTask(index);
        String response = shouldMarkDone ? "Nice! I've marked this task as done:\n  " + task
                : "OK, I've marked this task as not done yet:\n  " + task;
        return changed(response);
    }

    private CommandResult addTask(String command, String userInput) throws LilyException {
        Task task = switch (command) {
        case "todo" -> Parser.parseTodo(userInput);
        case "deadline" -> Parser.parseDeadline(userInput);
        case "event" -> Parser.parseEvent(userInput);
        default -> throw new IllegalArgumentException("Unsupported task command: " + command);
        };
        tasks.add(task);
        return changed("Got it. I've added this task:\n  " + task + "\nNow you have "
                + tasks.size() + " tasks in the list.");
    }

    private CommandResult deleteTask(String argument) throws LilyException {
        int index = parseRequiredTaskIndex(argument, "delete");
        if (!tasks.containsIndex(index)) {
            return unchanged("That task number does not exist.");
        }
        Task removed = tasks.remove(index);
        return changed("OK! I've removed this task:\n  " + removed + "\nNow you have "
                + tasks.size() + " tasks in the list.");
    }

    private CommandResult findTasks(String argument) throws LilyException {
        if (argument.isEmpty()) {
            throw new LilyException("Please provide a keyword to search for.");
        }
        return unchanged(formatTaskList(tasks.findTasks(argument), "Here are the matching tasks in your list:"));
    }

    private CommandResult changed(String response) {
        return new CommandResult(response, true);
    }

    private CommandResult unchanged(String response) {
        return new CommandResult(response, false);
    }

    private Task markTask(int index) {
        tasks.mark(index);
        return tasks.get(index);
    }

    private Task unmarkTask(int index) {
        tasks.unmark(index);
        return tasks.get(index);
    }

    private int parseRequiredTaskIndex(String argument, String command) throws LilyException {
        if (argument.isEmpty()) {
            throw new LilyException("Please provide a task number to " + command
                    + ", e.g. \"" + command + " 2\".");
        }
        return Parser.parseTaskIndex(argument);
    }

    private String formatTaskList(List<Task> taskList, String heading) {
        if (taskList.isEmpty()) {
            return "There are no matching tasks in your list.";
        }
        StringBuilder response = new StringBuilder(heading);
        for (int i = 0; i < taskList.size(); i++) {
            response.append("\n").append(i + 1).append(". ").append(taskList.get(i));
        }
        return response.toString();
    }

    /** Represents a command reply and whether it needs to be saved. */
    private record CommandResult(String response, boolean changedTaskList) {
    }

    /** Runs the read-command/act/respond loop until the user says {@code bye}. */
    public void run() {
        ui.showWelcome(WELCOME_MESSAGE);

        while (true) {
            String userInput = ui.readCommand();
            if (userInput == null) {
                ui.showResponse(GOODBYE_MESSAGE);
                break;
            }
            String response = getResponse(userInput);
            ui.showResponse(response);
            if (userInput.trim().equals("bye")) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        new Lily("data/lily.txt").run();
    }
}
