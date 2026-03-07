import java.util.Scanner;

public class Q4 {
    public static void main(String[] args) {
        float x;
        float n;

        if (args.length != 0) {             // input numbers via command-line arguments (space-separated)
            x = Float.parseFloat(args[0]);
            n = Float.parseFloat(args[1]);
        } else {                            // Scanner will be used if no arguments given
            Scanner scanner = new Scanner(System.in);
            System.out.print("Please enter the base number: ");
            x = scanner.nextInt();

            System.out.print("Please enter the exponent: ");
            n = scanner.nextInt();
        }

        // System.nanoTime() is used to compare the Naive and Squaring/Recursive implementations
        double startTimeNaive = System.nanoTime();
        System.out.println("Naive Result: " + expBySquaringNaive(x, n));
        double endTimeNaive = System.nanoTime();

        double runTimeNaive = (endTimeNaive - startTimeNaive) / 1000000;

        System.out.println("Runtime of Naive Implementation: " + runTimeNaive + "ms");

        double startTimeRecursive = System.nanoTime();
        System.out.println("Tail-Recursive Result: " + expBySquaringRecursive(1, x, n));
        double endTimeRecursive = System.nanoTime();

        double runTimeRecursive = (endTimeRecursive - startTimeRecursive) / 1000000;
        System.out.println("Runtime of Tail-Recursive Implementation: " + runTimeRecursive + "ms");
        System.out.println("Exponentiation-By-Squaring was " + (runTimeNaive - runTimeRecursive) + "ms faster");
    }

    public static float expBySquaringRecursive(float multiplier, float base, float exponent){
        // the method is called with an initial multiplier of 1 (Java does not allow default argument values)
        // expBySquaringRecursive uses the multiplier variable to keep track of the product that accumulates as a
        // result of odd exponents
        // the multiplier is used so that there is no arithmetic in the return statement, in order to satisfy the
        // tail-recursion requirement of the question
        if (exponent < 0) return expBySquaringRecursive(multiplier,1 / base, -exponent); // if the exponent is negative,
            // set the base equal to 1/base and make the exponent positive.
            // this will correctly calculate the decimal value of the result exponentiation
            // the possibility of a negative exponent is why this function's return type is float
        else if (exponent == 0) return 1; // when exponent = 0, return 1
        else if (exponent == 1) return multiplier * base; //when exponent = 1, return the accumulated multiplier * base
        else if (exponent % 2 == 0) return expBySquaringRecursive(multiplier, base * base, exponent / 2);
        // when the exponent is an even number, square the base, by replacing the base with base * base and
        // halving the exponent
        else return expBySquaringRecursive(multiplier * base,base * base, (exponent - 1) / 2);
        // when the exponent is odd accumulate the base^1 in the multiplier, requiring 1 to be taken from the exponent
        // then square the base, as any odd exponent != 1 is > 2, allowing exponent to be halved and base squared
    }
    
    public static float expBySquaringNaive(float base, float exponent){

        if (exponent < 0) {
            base = 1 / base;
            exponent = -exponent;
        }

        float result = base;
        for (int i = 2; i <= exponent; i++) {
           result = result * base;
        }
        return result;
    }
}


