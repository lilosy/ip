import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Saves and loads the task list from the application's data file.
 *
 * <p>Loading is designed to never crash the application: unreadable files are backed up
 * and a fresh list is returned, and individual corrupted lines are skipped (with a
 * warning) rather than aborting the whole load. Saving is done atomically so a crash or
 * power loss mid-write cannot leave behind a half-written, corrupted data file.
 */
public class Storage {
    private static final Path DATA_DIR = Path.of("data");
    private static final Path DATA_FILE = DATA_DIR.resolve("lily.txt");

    /** Literal delimiter used between fields in a saved record. */
    private static final String DELIMITER = " | ";
    private static final Pattern DELIMITER_SPLIT_PATTERN = Pattern.compile(Pattern.quote(DELIMITER));

    private Storage() {
        // Prevent instantiation; this class only exposes static utility methods.
    }

    /**
     * Escapes a field so it can safely be embedded in a {@value #DELIMITER}-separated
     * record even if it contains a backslash or pipe character.
     */
    public static String escapeField(String field) {
        if (field == null) {
            return "";
        }
        return field.replace("\\", "\\\\").replace("|", "\\|");
    }

    /** Reverses {@link #escapeField(String)}. */
    private static String unescapeField(String field) {
        return field.replace("\\|", "|").replace("\\\\", "\\");
    }

    /**
     * Replaces the data file with one parseable record for every task.
     *
     * <p>The write is atomic: tasks are first written to a temporary file in the same
     * directory, then moved into place, so a crash partway through a write cannot
     * corrupt or truncate the existing save file.
     *
     * @param tasks the tasks to save; must not be {@code null}, but may be empty
     * @throws IOException if the data directory or file cannot be created or written
     */
    public static void saveTasks(List<Task> tasks) throws IOException {
        if (tasks == null) {
            throw new IOException("Cannot save a null task list.");
        }

        try {
            createDataDirectory();
        } catch (FileAlreadyExistsException e) {
            throw new IOException("Cannot save tasks: '" + DATA_DIR
                    + "' exists but is not a directory. Please remove or rename it.", e);
        }

        List<String> taskRecords = new ArrayList<>();
        for (Task task : tasks) {
            if (task == null) {
                // Defensive: skip any null slot rather than let a NullPointerException
                // during toFileString() take down the whole save operation.
                continue;
            }
            taskRecords.add(task.toFileString());
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile(DATA_DIR, "lily", ".tmp");
            Files.write(tempFile, taskRecords, StandardCharsets.UTF_8);
            moveIntoPlace(tempFile);
        } catch (IOException e) {
            cleanupQuietly(tempFile);
            throw new IOException("Unable to save tasks to '" + DATA_FILE + "': " + e.getMessage(), e);
        }
    }

    private static void createDataDirectory() throws IOException {
        if (Files.exists(DATA_DIR) && !Files.isDirectory(DATA_DIR)) {
            throw new FileAlreadyExistsException(DATA_DIR.toString());
        }
        Files.createDirectories(DATA_DIR);
    }

    private static void moveIntoPlace(Path tempFile) throws IOException {
        try {
            Files.move(tempFile, DATA_FILE,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            // Some filesystems (e.g. certain network drives) don't support atomic moves.
            // Fall back to a plain (non-atomic) replace rather than failing the save.
            Files.move(tempFile, DATA_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void cleanupQuietly(Path tempFile) {
        if (tempFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException ignored) {
            // Best-effort cleanup only; nothing more we can do here.
        }
    }

    /**
     * Loads saved tasks, or returns an empty list when Lily has not saved any tasks yet.
     *
     * <p>This method is deliberately tolerant of a damaged save file so a corrupted or
     * partially-written file never prevents the chatbot from starting up:
     * <ul>
     *     <li>a missing file, or a missing data directory, simply yields an empty list;</li>
     *     <li>individual malformed lines are skipped (with a warning printed) so the
     *     rest of a mostly-valid file still loads;</li>
     *     <li>if the file cannot be read at all (bad permissions, wrong encoding,
     *     binary garbage, etc.), it is renamed aside as a timestamped backup and an
     *     empty list is returned so the user can keep using Lily.</li>
     * </ul>
     *
     * @return the tasks reconstructed from the data file (possibly empty)
     */
    public static List<Task> loadTasks() {
        if (Files.notExists(DATA_FILE)) {
            return new ArrayList<>();
        }

        if (!Files.isRegularFile(DATA_FILE)) {
            System.out.println("[Warning] '" + DATA_FILE
                    + "' is not a regular file; starting with an empty task list.");
            return new ArrayList<>();
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(DATA_FILE, StandardCharsets.UTF_8);
        } catch (MalformedInputException e) {
            backupCorruptedFile("not valid UTF-8 text");
            return new ArrayList<>();
        } catch (IOException e) {
            backupCorruptedFile(e.getMessage());
            return new ArrayList<>();
        }

        List<Task> tasks = new ArrayList<>();
        int lineNumber = 0;
        int skippedCount = 0;
        for (String taskRecord : lines) {
            lineNumber++;
            if (taskRecord == null || taskRecord.isBlank()) {
                continue;
            }
            try {
                tasks.add(parseTask(taskRecord));
            } catch (LilyException e) {
                System.out.println("[Warning] Skipping corrupted entry on line " + lineNumber
                        + " of '" + DATA_FILE + "': " + e.getMessage());
                skippedCount++;
            }
        }

        if (skippedCount > 0) {
            System.out.println("[Warning] " + skippedCount
                    + " corrupted task record(s) were ignored. The rest of your tasks loaded normally.");
        }
        return tasks;
    }

    /**
     * Moves an unreadable data file aside (with a timestamped suffix) so a fresh,
     * empty save file can take its place instead of the chatbot refusing to start.
     */
    private static void backupCorruptedFile(String reason) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = DATA_DIR.resolve("lily.txt.corrupted-" + timestamp);
        try {
            Files.move(DATA_FILE, backup, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[Warning] Could not read '" + DATA_FILE + "' (" + reason
                    + "). The unreadable file was backed up to '" + backup
                    + "' and Lily is starting with an empty task list.");
        } catch (IOException moveFailed) {
            System.out.println("[Warning] Could not read '" + DATA_FILE + "' (" + reason
                    + "), and it could not be backed up either (" + moveFailed.getMessage()
                    + "). Starting with an empty task list; the file was left untouched.");
        }
    }

    /**
     * Reconstructs one task from a record created by {@link Task#toFileString()}.
     *
     * @throws LilyException if the record is missing fields, has an unknown task type,
     *                        an invalid done-flag, or blank required fields
     */
    private static Task parseTask(String taskRecord) throws LilyException {
        String[] rawFields = DELIMITER_SPLIT_PATTERN.split(taskRecord, -1);
        if (rawFields.length < 3) {
            throw new LilyException("expected at least 3 fields separated by \" | \", found "
                    + rawFields.length);
        }

        String[] fields = new String[rawFields.length];
        for (int i = 0; i < rawFields.length; i++) {
            fields[i] = unescapeField(rawFields[i].trim());
        }

        String type = fields[0];
        String doneFlag = fields[1];
        if (!doneFlag.equals("0") && !doneFlag.equals("1")) {
            throw new LilyException("done-flag must be '0' or '1', found '" + doneFlag + "'");
        }

        Task task;
        switch (type) {
            case "T":
                requireFieldCount(fields, 3, "todo");
                requireNonBlank(fields, 2, "description");
                task = new ToDo(fields[2]);
                break;
            case "D":
                requireFieldCount(fields, 4, "deadline");
                requireNonBlank(fields, 2, "description");
                requireNonBlank(fields, 3, "'by' date");
                task = new Deadline(fields[2], DateTimeParser.parseStorageFormat(fields[3]));
                break;
            case "E":
                requireFieldCount(fields, 5, "event");
                requireNonBlank(fields, 2, "description");
                requireNonBlank(fields, 3, "'from' time");
                requireNonBlank(fields, 4, "'to' time");
                task = new Event(fields[2], DateTimeParser.parseStorageFormat(fields[3]),
                        DateTimeParser.parseStorageFormat(fields[4]));
                break;
            default:
                throw new LilyException("unknown task type '" + type + "' (expected T, D, or E)");
        }

        if (doneFlag.equals("1")) {
            task.markAsDone();
        }
        return task;
    }

    private static void requireFieldCount(String[] fields, int expected, String taskTypeName)
            throws LilyException {
        if (fields.length != expected) {
            throw new LilyException(taskTypeName + " record needs exactly " + expected
                    + " fields, found " + fields.length);
        }
    }

    private static void requireNonBlank(String[] fields, int index, String fieldName)
            throws LilyException {
        if (fields[index].isBlank()) {
            throw new LilyException(fieldName + " cannot be blank");
        }
    }
}
