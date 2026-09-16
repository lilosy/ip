package lily.task;

import java.time.LocalDateTime;

import lily.parser.DateTimeParser;
import lily.storage.Storage;

/** Represents a task that occurs during a time interval. */
public class Event extends Task {
    protected LocalDateTime from;
    protected LocalDateTime to;

    /**
     * Creates an event with the given description and time range.
     *
     * @param description description of the event
     * @param from        start date and time
     * @param to          end date and time
     */
    public Event(String description, LocalDateTime from, LocalDateTime to) {
        super(description);
        // The command parser establishes this interval invariant before creating the event.
        assert from != null && to != null : "An event must have both endpoints";
        assert !to.isBefore(from) : "An event cannot end before it starts";
        this.from = from;
        this.to = to;
    }

    @Override
    public String toString() {
        return "[E]" + super.toString() + " (from: " + DateTimeParser.formatForDisplay(this.from)
                + " to: " + DateTimeParser.formatForDisplay(this.to) + ")";
    }

    @Override
    public String toFileString() {
        return "E | " + (isDone ? "1" : "0") + " | " + Storage.escapeField(description)
                + " | " + DateTimeParser.formatForStorage(from)
                + " | " + DateTimeParser.formatForStorage(to);
    }

}
