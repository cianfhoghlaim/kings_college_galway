import java.util.Scanner;

public class Q3 {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.print("How long should the series be? ");
        int n = scanner.nextInt();
        if (n < 0) {
            System.out.println("A series can't have negative length.");
            System.exit(0);
        }

        // ask the user to choose the type of Fibonacci and output the result
        System.out.print("Enter 1 for iterative or 2 for recursive Fibonacci: ");
        int choice = scanner.nextInt();
        if (choice == 1) {
            System.out.print("Result: " );
            for (int i = 0; i < n; i++) {
                System.out.print(Fibonacci(i) + " ");
            }
        } else if (choice == 2) {
            System.out.print("Result: ");
            for (int i = 0; i < n; i++) {
                System.out.print(RecFibonacci(i) + " ");
            }
        } else {
            System.out.println("Please enter 1 or 2.");
        }
    }

    public static int Fibonacci(int n) {
        // the 0th and 1st term are 0 and 1, respectively
        if (n <= 1) {
            return n;
        }

        int prev2 = 0;
        int prev1 = 1;

        // if n = 2, the for loop is skipped
        for (int i = 2; i < n ; i++) {
            // when n > 2, the next number in the series is calculated as
            // the sum of the previous 2, the preious 2 numbers are updated until
            // the nth is reached
            int next = prev1 + prev2;
            prev2 = prev1;
            prev1 = next;
        }

        // return the fibonacci number as the sum of the previous 2 terms
        return prev1 + prev2;
    }

    public static int RecFibonacci(int n) {
        // the 0th and 1st term are 0 and 1, respectively
        if (n <= 1) {
            return n;
        }
        // the nth term is the sum of the (n-1)st and (n-1)nd terms, called recursively
        return RecFibonacci(n - 1) + RecFibonacci(n - 2);
    }


}
