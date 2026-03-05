import java.util.Scanner;

public class Temperature {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); //create scanner for user input
        System.out.println("Please choose a temperature to convert from:");
        System.out.println("1. Celsius");
        System.out.println("2. Fahrenheit");

        String choice = scanner.nextLine();
        System.out.print("Please input the degrees: ");
        double degrees = scanner.nextDouble();

        //account for different input choice formats
        if (choice.equals("1") || choice.equals("Celsius") || choice.equals("celsius")) {
            System.out.println("The Fahrenheit equivalent is: " + celsiusToFahrenheit(degrees)); //call conversion method
        } else if (choice.equals("2") || choice.equals("Fahrenheit") || choice.equals("fahrenheit")) {
            System.out.println("The Celsius equivalent is: " + fahrenheitToCelsius(degrees)); //call conversion method
        } else {
            System.out.println("Incorrect input. Please enter one of the available options.");
        }
    }

    public static double celsiusToFahrenheit (double fahrenheit) {
        return 5.0/9.0 * (fahrenheit - 32);
    }

    public static double fahrenheitToCelsius (double celsius) {
        return 9.0/5.0 * celsius + 32;
    }

}
