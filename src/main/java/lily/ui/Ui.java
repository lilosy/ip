package lily.ui;

import java.util.NoSuchElementException;
import java.util.Scanner;

import lily.task.Task;
import lily.task.TaskList;

/** Handles all console input and output for Lily. */
public class Ui {
    private static final String BANNER = " _     _ _       \n"
            + "| |   (_) |      \n"
            + "| |    _| |_   _ \n"
            + "| |   | | | | | |\n"
            + "| |___| | | |_| |\n"
            + "\\_____/_|\\__, |  \n"
            + "          __/ |  \n"
            + "         |___/   \n";
    private static final String DIVIDER = "----------------------------------------------------------";

    private final Scanner scanner;

    /** Creates a UI that reads commands from standard input. */
    public Ui() {
        scanner = new Scanner(System.in);
    }

    /** Shows Lily's welcome banner and initial prompt. */
    public void showWelcome() {
        System.out.println(BANNER);
        System.out.println("Hey there! I'm Lily.\nWhat would you like to do today?");
        showDivider();
    }

    /**
     * Reads and trims one command, or returns {@code null} when input has ended.
     */
    public String readCommand() {
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException | IllegalStateException e) {
            return null;
        }
    }

    /** Shows a separator after a command result. */
    public void showDivider() {
        System.out.println(DIVIDER);
    }

    /** Shows Lily's farewell message. */
    public void showGoodbye() {
        System.out.println();
        System.out.println("Bye! See you soon :)");
    }

    /** Shows every task in the supplied list. */
    public void showTaskList(TaskList tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println(String.format("%d. %s", i + 1, tasks.get(i)));
        }
    }

    /** Shows confirmation that a task was marked as done. */
    public void showTaskMarked(Task task) {
        System.out.println("Nice! I've marked this task as done:");
        System.out.println("  " + task);
    }

    /** Shows confirmation that a task was marked as not done. */
    public void showTaskUnmarked(Task task) {
        System.out.println(" OK, I've marked this task as not done yet:");
        System.out.println("  " + task);
    }

    /** Shows confirmation that a task was added and the updated task count. */
    public void showTaskAdded(Task task, TaskList tasks) {
        System.out.println("Got it. I've added this task:\n\t" + task);
        showTaskListSummary(tasks);
    }

    /** Shows confirmation that a task was deleted and the updated task count. */
    public void showTaskDeleted(Task task, TaskList tasks) {
        System.out.println("OK! I've removed this task:");
        System.out.println("\t" + task);
        showTaskListSummary(tasks);
    }

    /** Shows the number of tasks currently in the list. */
    public void showTaskListSummary(TaskList tasks) {
        System.out.println("Now you have " + tasks.size() + " tasks in the list");
    }

    /** Shows an error message produced while processing a command. */
    public void showError(Exception exception) {
        System.out.println(exception.getMessage());
    }

    /** Shows a message for a command Lily does not recognise. */
    public void showInvalidCommand() {
        System.out.println("invalid command");
    }

    /** Shows a message for a task number outside the current list. */
    public void showMissingTask() {
        System.out.println("That task number does not exist.");
    }

    /** Shows a message for a task number that is not an integer. */
    public void showInvalidTaskNumber() {
        System.out.println("Please provide a valid task number.");
    }

    /**
     * Shows an unexpected command-processing failure without ending the session.
     */
    public void showUnexpectedError(RuntimeException exception) {
        System.out.println(
                "Something went wrong handling that command: " + exception.getMessage());
    }
}
