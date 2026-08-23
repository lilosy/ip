import java.util.Scanner;

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
        Task[] tasks = new Task[100];
        int taskCount = 0;

        System.out.println(banner);
        System.out.println(openingMessage);

        while (true) {
            String userInput = myObj.nextLine().trim();  // Read user input
            if (userInput.equals("bye")) {
                break;
            } else if (userInput.equals("list")) {
                for (int i = 0; i < taskCount; i++) {
                    System.out.println(String.format("%d. %s", i + 1, tasks[i].toString()));
                }
            } else if (userInput.startsWith("mark ")) {
                String taskNumberText = userInput.substring(5);

                try {
                    int taskIndex = Integer.parseInt(taskNumberText) - 1;
                    if (taskIndex < 0 || taskIndex >= taskCount) {
                        System.out.println("That task number does not exist.");
                    } else {
                        Task task = tasks[taskIndex];
                        task.markAsDone();
                        System.out.println("Nice! I've marked this task as done:");
                        System.out.println("  " + task);
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Please provide a valid task number.");
                }

            } else if (userInput.startsWith("unmark ")) {
                String taskNumberText = userInput.substring(7);

                try {
                    int taskIndex = Integer.parseInt(taskNumberText) - 1;
                    if (taskIndex < 0 || taskIndex >= taskCount) {
                        System.out.println("That task number does not exist.");
                    } else {
                        Task task = tasks[taskIndex];
                        task.markAsUndone();
                        System.out.println(" OK, I've marked this task as not done yet:");
                        System.out.println("  " + task);
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Please provide a valid task number.");
                }
            } else {
                tasks[taskCount] = new Task(userInput);
                taskCount++;

                System.out.println("---------------------");
                System.out.println("added: " + userInput);
                System.out.println("---------------------");
            }

        }

        System.out.println();
        System.out.println(closingMessage);
    }
}
