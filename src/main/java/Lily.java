import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

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
        List<Task> tasks = new ArrayList<>();
        int taskCount = 0;

        System.out.println(banner);
        System.out.println(openingMessage);

        while (true) {
            String userInput = myObj.nextLine().trim();  // Read user input
            if (userInput.equals("bye")) {
                break;
            }
            String parts[] = userInput.split(" ", 2);
            String command = parts[0];

            if (command.equals("list")) {
                listTasks(tasks, taskCount);
            } else if (command.equals("mark")) {
                String taskNumberText = parts[1];
                markTask(tasks, taskNumberText);
            } else if (command.equals("unmark")) {
//                String taskNumberText = userInput.substring(7);
                String taskNumberText = parts[1];
                unmarkTask(tasks, taskNumberText);
            } else if (command.equals("todo")) {
                addTodo(tasks, userInput);
            } else if (command.equals("deadline")) {
                addDeadline(tasks, userInput);
            } else if (command.equals("event")) {
                addEvent(tasks, userInput);
            }
            else {

                System.out.println("---------------------");
                System.out.println("invalid command");
                System.out.println("---------------------");
            }

        }

        System.out.println();
        System.out.println(closingMessage);
    }

    public static void listTasks(List<Task> tasks, int taskCount) {
        for (int i = 0; i < tasks.size(); i++) {
            System.out.println(String.format("%d. %s", i + 1, tasks.get(i).toString()));
        }
    }

    public static void markTask(List<Task> tasks, String taskNum) {
        try {
            int taskIndex = Integer.parseInt(taskNum) - 1;
            if (taskIndex < 0 || taskIndex >= tasks.size()) {
                System.out.println("That task number does not exist.");
            } else {
                Task task = tasks.get(taskIndex);
                task.markAsDone();
                System.out.println("Nice! I've marked this task as done:");
                System.out.println("  " + task);
            }

        } catch (NumberFormatException e) {
            System.out.println("Please provide a valid task number.");
        }
    }

    public static void unmarkTask(List<Task> tasks, String taskNum) {
        try {
            int taskIndex = Integer.parseInt(taskNum) - 1;
            if (taskIndex < 0 || taskIndex >= tasks.size()) {
                System.out.println("That task number does not exist.");
            } else {
                Task task = tasks.get(taskIndex);
                task.markAsUndone();
                System.out.println(" OK, I've marked this task as not done yet:");
                System.out.println("  " + task);
            }

        } catch (NumberFormatException e) {
            System.out.println("Please provide a valid task number.");
        }
    }

    public static void addTodo(List<Task> tasks, String userInput) {
        String[] parts = userInput.split(" ", 2);
        tasks.add(new ToDo(parts[1]));
    }

    public static void addDeadline(List<Task> tasks, String userInput) {
        String[] parts = userInput.split(" ", 2);
        String deadlineTaskdDesc = parts[1];
        String[] deadlineParts = deadlineTaskdDesc.split(" /by ", 2);
        tasks.add(new Deadline(deadlineParts[0], deadlineParts[1]));
    }

    public static void addEvent(List<Task> tasks, String userInput) {
        String[] parts = userInput.split(" ", 2);
        String eventTaskDesc = parts[1];
        String[] eventParts = eventTaskDesc.split(" /from | /to ", 3);
        tasks.add(new Event(eventParts[0], eventParts[1], eventParts[2]));
    }
}
