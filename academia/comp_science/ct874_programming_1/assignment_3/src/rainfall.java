import java.util.Scanner;

public class Rainfall {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in); //Scanner used for user input

        double[] monthlyRainfall = new double[12];
        int i = 0;
        double total = 0;
        while (true) {
            //try-catch block to ensure valid input
            try{
                System.out.print("Enter rainfall in cm for month " + (i + 1) + ": ");
                double input = scanner.nextDouble();
                monthlyRainfall[i] = input; //add input to array
                total = total + input; //add input to the total
                System.out.println(monthlyRainfall[i]);
                i++;
                if (i == 12) {
                    break; //while loop will continue until 12 values stored
                }
            } catch (Exception e) {
                System.out.println("Please enter a valid numerical value for month " + (i + 1) +"'s rainfall.");
                scanner.next();
            }
        }

        double average = total / monthlyRainfall.length; //divide total by number of months to calculat average

        System.out.println("Annual Average Rainfall: " + average);

    }
}
