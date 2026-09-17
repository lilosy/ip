package lily.parser;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import lily.exception.LilyException;
import lily.task.Deadline;
import lily.task.Event;
import lily.task.Task;
import lily.task.TaskList;
import lily.task.ToDo;
import lily.ui.Ui;

/**
 * Makes sense of raw command text typed by the user.
 *
 * <p>
 * This class only interprets text: it turns a command line into a command word,
 * into
 * a validated task index, or into a fully-built {@link Task}. It never touches
 * the task
 * list or prints anything — those responsibilities belong to {@link TaskList}
 * and
 * {@link Ui} respectively, so a parsing rule only ever needs to change in one
 * place.
 */
public class Parser {
    private static final String COMMAND_SPACING_ERROR =
            "Use single spaces between words.";

    private Parser() {
        // Static utility class; no instances.
    }

    /**
     * Trims the outside of a complete command, validates its internal spacing, and
     * separates its command word from its arguments. Command words are
     * case-insensitive, while argument text retains its original case.
     *
     * @param userInput complete command typed by the user
     * @return the normalized command word and case-preserved argument text
     * @throws LilyException if the command uses repeated or non-space whitespace
     */
    public static ParsedCommand parseCommand(String userInput) throws LilyException {
        if (userInput == null || userInput.isBlank()) {
            throw new LilyException("Please enter a command.");
        }
        String trimmedInput = userInput.trim();
        if (trimmedInput.contains("  ")
                || trimmedInput.chars().anyMatch(character -> Character.isWhitespace(character)
                        && character != ' ')) {
            throw new LilyException(COMMAND_SPACING_ERROR);
        }

        String[] parts = trimmedInput.split(" ", 2);
        String commandWord = parts[0].toLowerCase(Locale.ROOT);
        String arguments = parts.length == 2 ? parts[1] : "";
        return new ParsedCommand(commandWord, arguments);
    }

    /**
     * Parses a 1-based task-number argument (as typed by the user) into a validated
     * 0-based index.
     *
     * @throws LilyException if the argument is not a valid integer
     */
    public static int parseTaskIndex(String argument) throws LilyException {
        if (argument == null || !argument.matches("[0-9]+")) {
            throw new LilyException("Please provide one positive whole-number task number.");
        }
        try {
            int oneBasedIndex = Integer.parseInt(argument);
            if (oneBasedIndex == 0) {
                throw new LilyException("Please provide one positive whole-number task number.");
            }
            return oneBasedIndex - 1;
        } catch (NumberFormatException e) {
            throw new LilyException("That task number is too large.");
        }
    }

    /** A command word paired with its original, case-preserved argument text. */
    public record ParsedCommand(String commandWord, String arguments) {
    }

    /** Parses a date argument for the schedule command. */
    public static LocalDate parseScheduleDate(String argument, LocalDate today) throws LilyException {
        return DateTimeParser.parseScheduleDate(argument, today);
    }

    /**
     * Builds the {@link ToDo} task described by a {@code todo <description>}
     * command.
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
     * @throws LilyException if the description, {@code /by} clause, or date is
     *                       missing
     *                       or malformed
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
        // The preceding check guarantees that both sides of the /by separator exist.
        assert deadlineParts.length == 2 : "A deadline command must split into description and date";

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
     * @throws LilyException if the description, {@code /from}/{@code /to} clauses,
     *                       or
     *                       either date is missing or malformed, or if {@code /to}
     *                       is before {@code /from}
     */
    public static Task parseEvent(String userInput) throws LilyException {
        String[] parts = userInput.split(" ", 2);
        if (parts.length < 2) {
            throw new LilyException("Add a description for the event");
        }

        String[] eventParts = splitEventDetails(parts[1]);
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

    /** Validates the event clauses and separates the description, start, and end text. */
    private static String[] splitEventDetails(String eventDetails) throws LilyException {
        if (!eventDetails.matches(".*\\s/from\\s.*\\s/to\\s.*")) {
            throw new LilyException(
                    "Wrong format for event, use this format: "
                            + "event [event desc] /from [...] /to [...]");
        }
        return eventDetails.split(" /from | /to ", 3);
    }
}
