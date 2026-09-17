package lily;

import java.io.IOException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import lily.exception.LilyException;
import lily.parser.DateTimeParser;
import lily.parser.Parser;
import lily.parser.Parser.ParsedCommand;
import lily.storage.Storage;
import lily.task.Deadline;
import lily.task.Event;
import lily.task.Task;
import lily.task.TaskList;
import lily.ui.Ui;

/** Runs Lily's command-line task manager. */
public class Lily {
    /** Greeting shared by both the command-line and graphical interfaces. */
    public static final String WELCOME_MESSAGE = "Hello, I'm Lily. Let's give your day a little room to grow.";
    /** Farewell shared by both the command-line and graphical interfaces. */
    public static final String GOODBYE_MESSAGE = "Take care. I'll keep your plans safe until you return.";
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
        if (input == null || input.isBlank()) {
            return "Please enter a command.";
        }

        try {
            CommandResult result = executeCommand(input);
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
        ParsedCommand parsedCommand = Parser.parseCommand(userInput);
        String command = parsedCommand.commandWord();
        String argument = parsedCommand.arguments();
        switch (command) {
        case "bye":
            requireNoArguments(command, argument);
            return unchanged(GOODBYE_MESSAGE);
        case "list":
            requireNoArguments(command, argument);
            return unchanged(formatTaskList(tasks.toList(), "Here's your little garden of tasks:"));
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
        case "schedule":
            requireAtMostOneArgument(command, argument);
            return showSchedule(argument);
        default:
            return unchanged("I’m not quite sure how to tend to that. Try `list`, `todo`, `deadline`, or `event`.");
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
        return changed("Planted it on your list:\n  " + task + "\nYou now have "
                + tasks.size() + " tasks to tend.");
    }

    private CommandResult deleteTask(String argument) throws LilyException {
        int index = parseRequiredTaskIndex(argument, "delete");
        if (!tasks.containsIndex(index)) {
            return unchanged("That task number does not exist.");
        }
        Task removed = tasks.remove(index);
        return changed("All cleared away:\n  " + removed + "\nYou now have "
                + tasks.size() + " tasks to tend.");
    }

    private CommandResult findTasks(String argument) throws LilyException {
        if (argument.isEmpty()) {
            throw new LilyException("Please provide a keyword to search for.");
        }
        requireSingleArgument("find", argument);
        return unchanged(formatTaskList(tasks.findTasks(argument), "Here are the matching tasks in your list:"));
    }

    /** Builds a read-only chronological view of dated tasks for one day. */
    private CommandResult showSchedule(String argument) throws LilyException {
        LocalDate selectedDate = Parser.parseScheduleDate(argument, LocalDate.now());
        List<ScheduleEntry> entries = findScheduleEntries(selectedDate);
        String displayedDate = DateTimeParser.formatForDisplay(selectedDate.atStartOfDay());
        if (entries.isEmpty()) {
            return unchanged("There are no scheduled tasks for " + displayedDate + ".");
        }

        StringBuilder response = new StringBuilder("Here is your schedule for ")
                .append(displayedDate).append(":");
        appendScheduleSection(response, "Not completed:", entries, false);
        appendScheduleSection(response, "Completed:", entries, true);
        return unchanged(response.toString());
    }

    /** Returns matching deadlines and events while retaining their list numbers. */
    private List<ScheduleEntry> findScheduleEntries(LocalDate selectedDate) {
        List<Task> taskSnapshot = tasks.toList();
        List<ScheduleEntry> entries = new ArrayList<>();
        for (int i = 0; i < taskSnapshot.size(); i++) {
            Task task = taskSnapshot.get(i);
            if (task instanceof Deadline deadline
                    && deadline.getBy().toLocalDate().equals(selectedDate)) {
                entries.add(new ScheduleEntry(i + 1, task,
                        deadline.getBy().toLocalTime().equals(LocalTime.MIDNIGHT),
                        deadline.getBy().toLocalTime()));
            } else if (task instanceof Event event
                    && !selectedDate.isBefore(event.getFrom().toLocalDate())
                    && !selectedDate.isAfter(event.getTo().toLocalDate())) {
                boolean hasNoDisplayedTime = event.getFrom().toLocalTime().equals(LocalTime.MIDNIGHT)
                        && event.getTo().toLocalTime().equals(LocalTime.MIDNIGHT);
                LocalTime effectiveTime = event.getFrom().toLocalDate().isBefore(selectedDate)
                        ? LocalTime.MIDNIGHT : event.getFrom().toLocalTime();
                entries.add(new ScheduleEntry(i + 1, task, hasNoDisplayedTime, effectiveTime));
            }
        }
        entries.sort(Comparator.comparingInt((ScheduleEntry entry) -> entry.hasNoDisplayedTime() ? 0 : 1)
                .thenComparing(ScheduleEntry::effectiveTime)
                .thenComparingInt(ScheduleEntry::taskNumber));
        return entries;
    }

    /** Appends one completion-status section, omitting it when there are no entries. */
    private void appendScheduleSection(StringBuilder response, String heading,
            List<ScheduleEntry> entries, boolean isDone) {
        boolean hasMatchingEntry = entries.stream().anyMatch(entry -> entry.task().isDone() == isDone);
        if (!hasMatchingEntry) {
            return;
        }
        response.append("\n").append(heading);
        entries.stream()
                .filter(entry -> entry.task().isDone() == isDone)
                .forEach(entry -> response.append("\n").append(entry.taskNumber())
                        .append(". ").append(entry.task()));
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

    /** Rejects arguments supplied to a command that accepts none. */
    private void requireNoArguments(String command, String argument) throws LilyException {
        if (!argument.isEmpty()) {
            throw new LilyException("The '" + command + "' command does not accept arguments.");
        }
    }

    /** Rejects commands whose optional/required argument contains another token. */
    private void requireAtMostOneArgument(String command, String argument) throws LilyException {
        if (argument.contains(" ")) {
            throw new LilyException("The '" + command + "' command accepts at most one argument.");
        }
    }

    /** Rejects a command argument containing more than one token. */
    private void requireSingleArgument(String command, String argument) throws LilyException {
        if (argument.contains(" ")) {
            throw new LilyException("The '" + command + "' command accepts exactly one argument.");
        }
    }

    private String formatTaskList(List<Task> taskList, String heading) {
        if (taskList.isEmpty()) {
            return "Your list is clear—a peaceful patch of soil.";
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

    /** A dated task together with its original number and per-day sorting data. */
    private record ScheduleEntry(int taskNumber, Task task, boolean hasNoDisplayedTime,
            LocalTime effectiveTime) {
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
            if (response.equals(GOODBYE_MESSAGE)) {
                break;
            }
        }
    }

    public static void main(String[] args) {
        new Lily("data/lily.txt").run();
    }
}
