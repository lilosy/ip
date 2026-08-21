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
        String[] tasks = new String[100];
        int taskCount = 0;

        System.out.println(banner);
        System.out.println(openingMessage);

        while (true) {
            String userInput = myObj.nextLine();  // Read user input
            if (userInput.equals("bye")) {
                break;
            } else if (userInput.equals("list")) {
                for (int i = 0; i < taskCount; i++) {
                    System.out.println(String.format("%d. %s", i + 1, tasks[i]));
                }
            } else {
                tasks[taskCount] = userInput;
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
