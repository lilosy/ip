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
 * Saves and loads the task list from a data file on disk.
 *
 * <p>Each instance is bound to one file path, supplied to the constructor (e.g.
 * {@code new Storage("data/lily.txt")}), so a caller could in principle point Lily at a
 * different save file without changing this class.
 *
 * <p>Loading is designed to never crash the application: unreadable files are backed up
 * and a fresh list is returned, and individual corrupted lines are skipped (with a
 * warning) rather than aborting the whole load. Saving is done atomically so a crash or
 * power loss mid-write cannot leave behind a half-written, corrupted data file.
 */
public class Storage {
    /** Literal delimiter used between fields in a saved record. */
    private static final String DELIMITER = " | ";
    private static final Pattern DELIMITER_SPLIT_PATTERN = Pattern.compile(Pattern.quote(DELIMITER));

    private final Path dataFile;
    private final Path dataDir;

    /**
     * Creates a Storage bound to the given file path.
     *
     * @param filePath path (relative or absolute) to the save file, e.g. {@code "data/lily.txt"}
     */
    public Storage(String filePath) {
        this.dataFile = Path.of(filePath);
        Path parent = dataFile.getParent();
        this.dataDir = (parent != null) ? parent : Path.of(".");
    }

    /**
     * Escapes a field so it can safely be embedded in a {@value #DELIMITER}-separated
     * record even if it contains a backslash or pipe character.
     *
     * <p>This stays a static utility (rather than an instance method) because escaping is
     * a pure text transformation that has nothing to do with any particular file; {@link
     * Task} subclasses call it directly while building their own save records.
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
    public void save(List<Task> tasks) throws IOException {
        if (tasks == null) {
            throw new IOException("Cannot save a null task list.");
        }

        try {
            createDataDirectory();
        } catch (FileAlreadyExistsException e) {
            throw new IOException("Cannot save tasks: '" + dataDir
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
            tempFile = Files.createTempFile(dataDir, "lily", ".tmp");
            Files.write(tempFile, taskRecords, StandardCharsets.UTF_8);
            moveIntoPlace(tempFile);
        } catch (IOException e) {
            cleanupQuietly(tempFile);
            throw new IOException("Unable to save tasks to '" + dataFile + "': " + e.getMessage(), e);
        }
    }

    private void createDataDirectory() throws IOException {
        if (Files.exists(dataDir) && !Files.isDirectory(dataDir)) {
            throw new FileAlreadyExistsException(dataDir.toString());
        }
        Files.createDirectories(dataDir);
    }

    private void moveIntoPlace(Path tempFile) throws IOException {
        try {
            Files.move(tempFile, dataFile,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            // Some filesystems (e.g. certain network drives) don't support atomic moves.
            // Fall back to a plain (non-atomic) replace rather than failing the save.
            Files.move(tempFile, dataFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void cleanupQuietly(Path tempFile) {
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
    public List<Task> load() {
        if (Files.notExists(dataFile)) {
            return new ArrayList<>();
        }

        if (!Files.isRegularFile(dataFile)) {
            System.out.println("[Warning] '" + dataFile
                    + "' is not a regular file; starting with an empty task list.");
            return new ArrayList<>();
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(dataFile, StandardCharsets.UTF_8);
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
                        + " of '" + dataFile + "': " + e.getMessage());
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
    private void backupCorruptedFile(String reason) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path backup = dataDir.resolve(dataFile.getFileName() + ".corrupted-" + timestamp);
        try {
            Files.move(dataFile, backup, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("[Warning] Could not read '" + dataFile + "' (" + reason
                    + "). The unreadable file was backed up to '" + backup
                    + "' and Lily is starting with an empty task list.");
        } catch (IOException moveFailed) {
            System.out.println("[Warning] Could not read '" + dataFile + "' (" + reason
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
