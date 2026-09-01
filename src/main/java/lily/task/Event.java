package lily.task;
import java.time.LocalDateTime;
import lily.parser.DateTimeParser;
import lily.storage.Storage;

public class Event extends Task {
    protected LocalDateTime from;
    protected LocalDateTime to;

    public Event(String description, LocalDateTime from, LocalDateTime to) {
        super(description);
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
