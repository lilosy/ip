import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Saves the current task list to the application's data file. */
public class Storage {
    private static final Path DATA_FILE = Path.of("data", "lily.txt");

    /**
     * Replaces the data file with one parseable record for every task.
     *
     * @param tasks the tasks to save
     * @throws IOException if the data directory or file cannot be written
     */
    public static void saveTasks(List<Task> tasks) throws IOException {
        Files.createDirectories(DATA_FILE.getParent());
        List<String> taskRecords = tasks.stream()
                .map(Task::toFileString)
                .toList();
        Files.write(DATA_FILE, taskRecords, StandardCharsets.UTF_8);
    }
}
