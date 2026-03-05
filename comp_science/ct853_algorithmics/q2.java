import java.util.Scanner;

public class Q2 {

    public static void main(String[] args) {
        float[] numbers; // this array will be filled with input from the console
        boolean subSort = false;

        if (args.length != 0) {             // input numbers via command-line arguments (space-separated)
            numbers = new float[args.length];

            for (int i = 0; i < numbers.length; i++) {
                numbers[i] = Integer.parseInt(args[i]); // populate array with user's input
            }

        } else {                            // Scanner will be used if no arguments given
            Scanner scanner = new Scanner(System.in);
            System.out.print("Please enter the length of the array of numbers: ");

            // find length of user's array
            int length = scanner.nextInt();
            if (length <= 0) {
                System.out.println("Length must be a positive number.");
                System.exit(0);
            }

            numbers = new float[length];

            System.out.print("Please enter the list of numbers, separated by spaces: ");
            for (int i = 0; i < length; i++) {
                numbers[i] = scanner.nextInt(); // populate array with user's input
            }
            scanner.nextLine(); //consume the /n from user input left behind by scanner.nextInt()

            // give the choice to specify array indices
            System.out.print("Would you like to sort a subsection of the array (Y/N)? ");
            String response = scanner.nextLine();

            if (response.equals("Y")) {
                System.out.print("Please enter the subarray indices, separated by spaces: ");

                int l = scanner.nextInt();
                int r = scanner.nextInt();
                subSort = true;
                mergeSort(numbers, l, r);
            }
        }

        if (!subSort) {
            mergeSort(numbers, 0, numbers.length - 1); // Java is pass by value, but for an array the value is its
            // memory
            // address
            // the changes made by the mergeSort method apply to the original array
            // without returning a value
        }

        // Output the result
        System.out.print("Result: ");
        for (int i = 0; i < numbers.length; i++) {
            System.out.print(numbers[i] + " ");
        }
    }

    public static void mergeSort(float[] numbers, int start, int end) {
        if (start < end) {
            int middle = start + (end - start) / 2; //calculte the middle index

            // use the middle index to break the specified range into two subproblems
            // with half the range
            mergeSort(numbers, start, middle);
            mergeSort(numbers, middle + 1, end);
            // merge these two sub problems
            merge(numbers, start, middle, end);
        }
    }

    public static void merge(float[] numbers, int start, int middle, int end) {
       // copy the array elements into two sub arrays of half the size + 1
        int lLength = middle - start + 1;
        int rLength = end - middle;

        float[] left = new float[lLength + 1];
        float[] right = new float[rLength + 1];

        for (int i = 0; i < lLength; i++) {
            left[i] = numbers[start + i];
            System.out.println(left[i]);
        }
        for (int j = 0; j < rLength; j++) {
            right[j] = numbers[middle + 1 + j];
            System.out.println(right[j]);
        }

        // last index is filled with the max possible value
        // to indicate to the following if statement that the array is full
        left[left.length - 1] = Float.MAX_VALUE;
        right[right.length - 1] = Float.MAX_VALUE;


        //lCounter and rCounter are used to count the elements of each temporary array that have alreayd been copied
        int lCounter = 0;
        int rCounter = 0;

        //k is used to count the elements elements successfully copied from the temporary arrays

        for (int k = start; k <= end; k++) {
            if (left[lCounter] <= right[rCounter]) {
                numbers[k] = left[lCounter];
                lCounter++;
            } else {
                numbers[k] = right[rCounter];
                rCounter++;
            }
        }
    }
}

