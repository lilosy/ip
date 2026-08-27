import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/** Runs Lily's command-line task manager. */
public class Lily {

    public static void main(String[] args) {
        String banner = " _     _ _       \n"
                + "| |   (_) |      \n"
                + "| |    _| |_   _ \n"
                + "| |   | | | | | |\n"
                + "| |___| | | |_| |\n"
                + "\\_____/_|\\__, |  \n"
                + "          __/ |  \n"
                + "         |___/   \n";
        Scanner myObj = new Scanner(System.in);  // Create a Scanner object
        String openingMessage = "Hey there! I'm Lily.\nWhat would you like to do today?";
        String closingMessage = "Bye! See you soon :)";
        String divider = "----------------------------------------------------------";
        List<Task> tasks = new ArrayList<>();

        System.out.println(banner);
        System.out.println(openingMessage);
        System.out.println(divider);

        while (true) {
            String userInput = myObj.nextLine().trim();  // Read user input
            if (userInput.equals("bye")) {
                break;
            }
            String parts[] = userInput.split(" ", 2);
            String command = parts[0];
            try {
                boolean taskListChanged = false;
                if (command.equals("list")) {
                    listTasks(tasks);
                } else if (command.equals("mark")) {
                    String taskNumberText = parts[1];
                    taskListChanged = markTask(tasks, taskNumberText);
                } else if (command.equals("unmark")) {
                    String taskNumberText = parts[1];
                    taskListChanged = unmarkTask(tasks, taskNumberText);
                } else if (command.equals("todo")) {
                    addTodo(tasks, userInput);
                    taskListChanged = true;
                } else if (command.equals("deadline")) {
                    addDeadline(tasks, userInput);
                    taskListChanged = true;
                } else if (command.equals("event")) {
                    addEvent(tasks, userInput);
                    taskListChanged = true;
                } else if (command.equals("delete")) {
                    taskListChanged = deleteTask(tasks, userInput);
                }
                else {
                    System.out.println("invalid command");
                }
                if (taskListChanged) {
                    Storage.saveTasks(tasks);
                }
                System.out.println(divider);
            } catch (LilyException | IOException e) {
                System.out.println(e.getMessage());
                System.out.println(divider);
            }

        }

        System.out.println();
        System.out.println(closingMessage);
    }

    /** Prints every task in the current list. */
    public static void listTasks(List<Task> tasks) {
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println(String.format("%d. %s", i + 1, tasks.get(i).toString()));
        }
    }

    /** Marks the requested task as done and reports whether it was found. */
    public static boolean markTask(List<Task> tasks, String taskNum) {
        try {
            int taskIndex = Integer.parseInt(taskNum) - 1;
            if (taskIndex < 0 || taskIndex >= tasks.size()) {
                System.out.println("That task number does not exist.");
                return false;
            } else {
                Task task = tasks.get(taskIndex);
                task.markAsDone();
                System.out.println("Nice! I've marked this task as done:");
                System.out.println("  " + task);
                return true;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please provide a valid task number.");
            return false;
        }
    }

    /** Marks the requested task as not done and reports whether it was found. */
    public static boolean unmarkTask(List<Task> tasks, String taskNum) {
        try {
            int taskIndex = Integer.parseInt(taskNum) - 1;
            if (taskIndex < 0 || taskIndex >= tasks.size()) {
                System.out.println("That task number does not exist.");
                return false;
            } else {
                Task task = tasks.get(taskIndex);
                task.markAsUndone();
                System.out.println(" OK, I've marked this task as not done yet:");
                System.out.println("  " + task);
                return true;
            }

        } catch (NumberFormatException e) {
            System.out.println("Please provide a valid task number.");
            return false;
        }
    }

    public static void addTodo(List<Task> tasks, String userInput) throws LilyException {
        String[] parts = userInput.split(" ", 2);
        if (parts.length < 2) {
            throw new LilyException("Add a description for the todo task");
        }
        Task newTask = new ToDo(parts[1]);
        tasks.add(newTask);
        System.out.println("Got it. I've added this task:\n\t" + newTask.toString());
        printTaskListSummary(tasks);
    }

    public static void addDeadline(List<Task> tasks, String userInput) throws LilyException{
        String[] parts = userInput.split(" ", 2);
        if (parts.length < 2) {
            throw new LilyException("Add a description for the deadline task");
        }
        String deadlineTaskdDesc = parts[1];
        String[] deadlineParts = deadlineTaskdDesc.split(" /by ", 2);
        if (deadlineParts.length < 2) {
            throw new LilyException("Add a deadline for the task");
        }
        Task newTask = new Deadline(deadlineParts[0], deadlineParts[1]);
        tasks.add(newTask);
        System.out.println("Got it. I've added this task:\n\t" + newTask.toString());
        printTaskListSummary(tasks);
    }

    public static void addEvent(List<Task> tasks, String userInput) throws LilyException {
        String[] parts = userInput.split(" ", 2);
        if (parts.length < 2) {
            throw new LilyException("Add a description for the event");
        }
        if(!parts[1].matches(".*\\s/from\\s.*\\s/to\\s.*")) {
            throw new LilyException("Wrong format for event, use this format: event [event desc] /from [...] /to [...]");
        }
        String eventTaskDesc = parts[1];
        String[] eventParts = eventTaskDesc.split(" /from | /to ", 3);
        Task newTask = new Event(eventParts[0], eventParts[1], eventParts[2]);
        tasks.add(newTask);
        System.out.println("Got it. I've added this task:\n\t" + newTask.toString());
        printTaskListSummary(tasks);
    }

    /** Removes the requested task and reports whether the task list changed. */
    public static boolean deleteTask(List<Task> tasks, String userInput) throws LilyException{
        String parts[] = userInput.split(" ", 2);
        if (parts.length != 2) {
            throw new LilyException("Wrong format for delete, please enter as such: delete taskNumber");
        }
        try {
            int taskIndex = Integer.parseInt(parts[1]) - 1;
            if (taskIndex < 0 || taskIndex >= tasks.size()) {
                System.out.println("That task number does not exist.");
                return false;
            } else {
                Task task = tasks.get(taskIndex);
                tasks.remove(taskIndex);
                System.out.println("OK! I've removed this task:");
                System.out.println("\t" + task);
                printTaskListSummary(tasks);
                return true;
            }
        } catch (NumberFormatException e) {
            System.out.println("Please provide a valid task number.");
            return false;
        }

    }

    public static void printTaskListSummary(List<Task> tasks) {
        System.out.println("Now you have " + tasks.size() + " tasks in the list");
    }
}
