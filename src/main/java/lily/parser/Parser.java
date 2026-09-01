package lily.parser;
import java.time.LocalDateTime;

import lily.task.Task;
import lily.task.ToDo;
import lily.task.Deadline;
import lily.task.Event;
import lily.exception.LilyException;
import lily.task.TaskList;
import lily.ui.Ui;

/**
 * Makes sense of raw command text typed by the user.
 *
 * <p>This class only interprets text: it turns a command line into a command word, into
 * a validated task index, or into a fully-built {@link Task}. It never touches the task
 * list or prints anything — those responsibilities belong to {@link TaskList} and
 * {@link Ui} respectively, so a parsing rule only ever needs to change in one place.
 */
public class Parser {

    private Parser() {
        // Static utility class; no instances.
    }

    /** Returns the command word: the first whitespace-separated token of the input. */
    public static String getCommandWord(String userInput) {
        return userInput.split(" ", 2)[0];
    }

    /** Returns everything after the command word, trimmed, or {@code ""} if there is none. */
    public static String getArguments(String userInput) {
        String[] parts = userInput.split(" ", 2);
        return parts.length > 1 ? parts[1].trim() : "";
    }

    /**
     * Parses a 1-based task-number argument (as typed by the user) into a validated
     * 0-based index.
     *
     * @throws LilyException if the argument is not a valid integer
     */
    public static int parseTaskIndex(String argument) throws LilyException {
        try {
            return Integer.parseInt(argument) - 1;
        } catch (NumberFormatException e) {
            throw new LilyException("Please provide a valid task number.");
        }
    }

    /**
     * Builds the {@link ToDo} task described by a {@code todo <description>} command.
     *
     * @throws LilyException if no description was supplied
     */
    public static Task parseTodo(String userInput) throws LilyException {
        String[] parts = userInput.split(" ", 2);
        if (parts.length < 2) {
            throw new LilyException("Add a description for the todo task");
        }
        return new ToDo(parts[1]);
    }

    /**
     * Builds the {@link Deadline} task described by a
     * {@code deadline <description> /by <date>} command.
     *
     * @throws LilyException if the description, {@code /by} clause, or date is missing
     *                        or malformed
     */
    public static Task parseDeadline(String userInput) throws LilyException {
        String[] parts = userInput.split(" ", 2);
        if (parts.length < 2) {
            throw new LilyException("Add a description for the deadline task");
        }
        String deadlineTaskDesc = parts[1];
        String[] deadlineParts = deadlineTaskDesc.split(" /by ", 2);
        if (deadlineParts.length < 2) {
            throw new LilyException("Add a deadline for the task");
        }
        String description = deadlineParts[0].trim();
        if (description.isEmpty()) {
            throw new LilyException("Add a description for the deadline task");
        }
        LocalDateTime by = DateTimeParser.parseUserInput(deadlineParts[1]);
        return new Deadline(description, by);
    }

    /**
     * Builds the {@link Event} task described by an
     * {@code event <description> /from <start> /to <end>} command.
     *
     * @throws LilyException if the description, {@code /from}/{@code /to} clauses, or
     *                        either date is missing or malformed, or if {@code /to} is
     *                        before {@code /from}
     */
    public static Task parseEvent(String userInput) throws LilyException {
        String[] parts = userInput.split(" ", 2);
        if (parts.length < 2) {
            throw new LilyException("Add a description for the event");
        }
        if (!parts[1].matches(".*\\s/from\\s.*\\s/to\\s.*")) {
            throw new LilyException(
                    "Wrong format for event, use this format: event [event desc] /from [...] /to [...]");
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
        return new Event(description, from, to);
    }
}
