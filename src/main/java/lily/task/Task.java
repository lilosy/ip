package lily.task;

import lily.storage.Storage;

/** Represents a task in Lily's to-do list. */
public class Task {
    protected String description;
    protected boolean isDone;

    /**
     * Creates a task with the given description.
     *
     * @param description words that describe the task
     */
    public Task(String description) {
        this.description = description;
        this.isDone = false;
    }

    /** Returns the status icon used to display the task's done state. */
    public String getStatusIcon() {
        return (isDone ? "X" : " "); // mark done task with X
    }

    /** Marks this task as done. */
    public void markAsDone() {
        this.isDone = true;
    }

    /** Marks this task as not yet done. */
    public void markAsUndone() {
        this.isDone = false;
    }

    /** Returns this task as a record suitable for saving to disk. */
    public String toFileString() {
        return "T | " + (isDone ? "1" : "0") + " | " + Storage.escapeField(description);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", this.getStatusIcon(), this.description);
    }
}
