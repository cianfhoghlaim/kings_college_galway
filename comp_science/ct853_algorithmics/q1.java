import java.util.Scanner;

public class Q1 {

    public static void main(String[] args) {

        float[] numbers; // this array will be filled with input from the console

        if (args.length != 0) {             // input numbers via command-line arguments (space-separated)
            numbers = new float[args.length];
            for (int i = 0; i < numbers.length; i++) {
                numbers[i] = Integer.parseInt(args[i]);
            }
        } else {                            // Scanner will be used if no arguments given
            Scanner scanner = new Scanner(System.in);
            System.out.print("Please enter the length of the array of numbers: ");
            int length = scanner.nextInt();
            if (length <= 0) {
                System.out.println("Length must be a positive number.");
                System.exit(0);
            }
            numbers = new float[length];

            System.out.print("Please enter the list of numbers, separated by spaces: ");
            for (int i = 0; i < length; i++) {
                numbers[i] = scanner.nextFloat();
            }
        }

        bubbleSort(numbers); // Java is pass by value, but for an array the value is its memory address
        // the changes made by the bubbleSort method apply to the original array
        // without returning a value

        // Output the result
        System.out.println("Sorted array: ");
        for (int i = 0; i < numbers.length; i++) {
            System.out.print(numbers[i] + " ");
        }
    }

    public static void bubbleSort(float[] numbers) {

        // the inner loop here will find the minimum element in the range
        // by comparing adjacent elements in the range [n - 1 ... i] and place it at i
        for (int i = 0; i < numbers.length - 2; i++) {
            for (int j = numbers.length - 1; j > i; j--) {
                if (numbers[j] < numbers[j - 1]) {
                    float temp = numbers[j];
                    numbers[j] = numbers[j - 1];
                    numbers[j - 1] = temp;
                }
            }
        }
    }

    public static void improvedBubbleSort(float[] numbers) {
        boolean swapped = false;
        for (int i = 0; i < numbers.length - 2; i++) {
            for (int j = numbers.length - 1; j > i; j--) {
                if (numbers[j] < numbers[j - 1]) {
                    float temp = numbers[j];
                    numbers[j] = numbers[j - 1];
                    numbers[j - 1] = temp;
                    swapped = true;
                }
            }
            // if no exchange is made the algorithm quits after one pass as the array is already sorted
            if (!swapped) {
                break;
            }
        }
    }
}
