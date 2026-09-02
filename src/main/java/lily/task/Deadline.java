package lily.task;

import java.time.LocalDateTime;

import lily.parser.DateTimeParser;
import lily.storage.Storage;

/** Represents a task that must be completed by a specific date/time. */
public class Deadline extends Task {
    protected LocalDateTime by;

    /**
     * Creates a deadline with the given description and due time.
     *
     * @param description description of the task
     * @param by          date and time by which the task must be completed
     */
    public Deadline(String description, LocalDateTime by) {
        super(description);
        this.by = by;
    }

    @Override
    public String toString() {
        return "[D]" + super.toString() + " (by: " + DateTimeParser.formatForDisplay(by) + ")";
    }

    @Override
    public String toFileString() {
        return "D | " + (isDone ? "1" : "0") + " | " + Storage.escapeField(description)
                + " | " + DateTimeParser.formatForStorage(by);
    }
}
