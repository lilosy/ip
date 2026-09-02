package lily.parser;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Locale;

import lily.exception.LilyException;
import lily.task.Deadline;
import lily.task.Event;
import lily.task.Task;
import lily.task.ToDo;

/**
 * Parses user-supplied date/time text (for deadlines and events) into
 * {@link LocalDateTime},
 * and formats {@link LocalDateTime} values for saving to disk or displaying to
 * the user.
 *
 * <p>
 * All date/time logic for the app lives here, in one place, split into three
 * distinct responsibilities that intentionally use different formats:
 * <ul>
 * <li><b>input</b> — flexible; several formats a user might type are
 * accepted;</li>
 * <li><b>storage</b> — fixed and unambiguous; used only in the save file, never
 * shown to the user, so it never needs to change even if input/display formats
 * do;</li>
 * <li><b>display</b> — human-friendly; used only when printing to the
 * console.</li>
 * </ul>
 *
 * <p>
 * Accepted input formats:
 * <ul>
 * <li>{@code yyyy-MM-dd} e.g. {@code 2019-10-15}</li>
 * <li>{@code yyyy-MM-dd HHmm} e.g. {@code 2019-10-15 1800}</li>
 * <li>{@code d/M/yyyy} e.g. {@code 2/12/2019}</li>
 * <li>{@code d/M/yyyy HHmm} e.g. {@code 2/12/2019 1800}</li>
 * </ul>
 * A date given without a time is treated as midnight (00:00), and the display
 * formatter
 * hides that midnight time again so date-only input round-trips cleanly.
 */
public class DateTimeParser {

    /**
     * Formats a user is allowed to type when entering a deadline/event date.
     *
     * <p>
     * STRICT resolution is used deliberately: Java's default (SMART) resolver
     * silently "fixes" impossible dates such as 31 February into 28 February, which
     * would make Lily accept bad input as if it were valid. STRICT rejects it
     * instead,
     * so an impossible date correctly falls through to the "couldn't understand"
     * error.
     *
     * <p>
     * Patterns use {@code uuuu} (proleptic year), not {@code yyyy} (year-of-era):
     * under STRICT resolution {@code yyyy} requires an explicit era (BC/AD) to
     * resolve
     * at all, which plain calendar years like "2019" never supply, so {@code yyyy}
     * plus
     * STRICT would reject every date. {@code uuuu} has no such requirement.
     */
    private static final DateTimeFormatter[] INPUT_FORMATS = {
            strict("uuuu-MM-dd HHmm"),
            strict("uuuu-M-d HHmm"),
            strict("uuuu-MM-dd"),
            strict("uuuu-M-d"),
            strict("d/M/uuuu HHmm"),
            strict("d/M/uuuu"),
    };

    private static DateTimeFormatter strict(String pattern) {
        return DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT);
    }

    /**
     * Fixed, unambiguous format used only for the on-disk save file. Never shown to
     * the user.
     */
    private static final DateTimeFormatter STORAGE_FORMAT = strict("uuuu-MM-dd'T'HH:mm");

    /** Human-friendly formats used only when printing to the console. */
    private static final DateTimeFormatter DISPLAY_DATE_ONLY = DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.US);
    private static final DateTimeFormatter DISPLAY_DATE_TIME = DateTimeFormatter.ofPattern("MMM dd yyyy, h:mma",
            Locale.US);

    private DateTimeParser() {
        // Static utility class; no instances.
    }

    /**
     * Parses user-typed text such as {@code "2019-10-15"} or
     * {@code "2/12/2019 1800"}
     * into a {@link LocalDateTime}. A date with no time component is taken to mean
     * midnight.
     *
     * @param rawInput the raw text typed after {@code /by}, {@code /from}, or
     *                 {@code /to}
     * @throws LilyException if the text is blank, does not match any accepted
     *                       format,
     *                       or describes an impossible date (e.g. 31 February)
     */
    public static LocalDateTime parseUserInput(String rawInput) throws LilyException {
        if (rawInput == null || rawInput.isBlank()) {
            throw new LilyException("A date/time is required.");
        }
        String trimmed = rawInput.trim();

        for (DateTimeFormatter format : INPUT_FORMATS) {
            try {
                TemporalAccessor parsed = format.parse(trimmed);
                LocalDate date = LocalDate.from(parsed);
                LocalTime time = parsed.isSupported(ChronoField.HOUR_OF_DAY)
                        ? LocalTime.from(parsed)
                        : LocalTime.MIDNIGHT;
                return LocalDateTime.of(date, time);
            } catch (DateTimeException ignored) {
                // Covers both DateTimeParseException (text doesn't match the pattern) and
                // a plain DateTimeException (text matches the pattern but describes an
                // impossible date/time, e.g. 31 February). Either way: try the next format.
            }
        }

        throw new LilyException("I couldn't understand the date/time '" + rawInput
                + "'. Try formats like: 2019-10-15, 2019-10-15 1800, or 2/12/2019 1800.");
    }

    /**
     * Formats a date/time for storage on disk. Fixed and unambiguous; never shown
     * to the user.
     */
    public static String formatForStorage(LocalDateTime dateTime) {
        return dateTime.format(STORAGE_FORMAT);
    }

    /**
     * Parses text previously produced by {@link #formatForStorage(LocalDateTime)}.
     *
     * @throws LilyException if the text is not a validly-formatted storage record
     */
    public static LocalDateTime parseStorageFormat(String storedText) throws LilyException {
        try {
            return LocalDateTime.parse(storedText.trim(), STORAGE_FORMAT);
        } catch (DateTimeParseException e) {
            throw new LilyException("invalid date/time '" + storedText + "'");
        }
    }

    /**
     * Formats a date/time for display to the user, hiding the time when it's
     * midnight.
     */
    public static String formatForDisplay(LocalDateTime dateTime) {
        return dateTime.toLocalTime().equals(LocalTime.MIDNIGHT)
                ? dateTime.format(DISPLAY_DATE_ONLY)
                : dateTime.format(DISPLAY_DATE_TIME);
    }
}
