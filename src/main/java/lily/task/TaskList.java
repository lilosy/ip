package lily.task;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns Lily's ordered collection of tasks and provides its basic task operations.
 *
 * <p>The class deliberately does not print messages. Command-specific feedback belongs to
 * the user-interface layer, while this class focuses only on changing or retrieving tasks.
 */
public class TaskList {
    private final List<Task> tasks;

    /** Creates an empty task list. */
    public TaskList() {
        this.tasks = new ArrayList<>();
    }

    /** Creates a task list populated with the supplied saved tasks. */
    public TaskList(List<Task> tasks) {
        this.tasks = new ArrayList<>(tasks);
    }

    /** Adds a task to the end of the list. */
    public void add(Task task) {
        tasks.add(task);
    }

    /** Returns the task at the given zero-based index. */
    public Task get(int index) {
        return tasks.get(index);
    }

    /** Removes and returns the task at the given zero-based index. */
    public Task remove(int index) {
        return tasks.remove(index);
    }

    /** Marks the task at the given zero-based index as done. */
    public void mark(int index) {
        tasks.get(index).markAsDone();
    }

    /** Marks the task at the given zero-based index as not done. */
    public void unmark(int index) {
        tasks.get(index).markAsUndone();
    }

    /** Returns whether the index identifies an existing task. */
    public boolean containsIndex(int index) {
        return index >= 0 && index < tasks.size();
    }

    /** Returns the number of tasks currently held. */
    public int size() {
        return tasks.size();
    }

    /** Returns a snapshot suitable for passing to persistence code. */
    public List<Task> toList() {
        return new ArrayList<>(tasks);
    }
}
