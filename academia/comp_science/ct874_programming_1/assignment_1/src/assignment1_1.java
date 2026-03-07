import java.util.Scanner; //import Scanner to allow for its instantiation

public class Assignment1_1 {
    public static void main(String[] args) {
        System.out.println("This program counts the number of digits in an integer entered by the user.");
        System.out.println("Enter -1 to exit.");

        Scanner scanner = new Scanner(System.in); //create a new scanner object with system.in as input

        while (true) {
            System.out.print("Enter integer: ");
            int input = scanner.nextInt(); //save user's input

            if (input == -1) {
                System.out.println("Program terminated...");
                break; //repeat loop until -1 is entered
            } else { //if input is != -1, output the number of digits
                System.out.println("Number of digits in " + input + " is " + String.valueOf(input).length());
            }
        }
    }
}
