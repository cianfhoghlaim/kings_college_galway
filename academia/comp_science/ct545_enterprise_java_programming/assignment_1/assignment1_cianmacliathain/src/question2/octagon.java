package question2;

import java.util.Scanner;

public class Octagon extends GeometricFigure2 implements ConsoleIO, ComparableGeometricFigure<Octagon>{
    private double length;

    // added a parameterised constructor to allow for length to be specified during question2.Octagon instantiation
    // a default constructor must then be added manually to keep that functionality

    public Octagon() {

    }

    public Octagon(double length) {
        this.length = length;
    }

    @Override
    public double calcArea() {
        // calculates the area using the sqrt method of the Math class, available
        // by default from the java.lang package
        return 2 * (1 + Math.sqrt(2)) * length * length;
    }

    @Override
    public void updateFromConsole() {
        //uses Scanner to prompt user for side length via console
        Scanner scanner = new Scanner(System.in);
        System.out.print("What is the octagon's side length? ");
        this.length = scanner.nextFloat();
    }

    @Override
    public void writeToConsole() {
        // prints the field values and the area of the octagon
        System.out.println("Side length: " + this.length + "\n" + "Area: " + calcArea());
    }

    public int compareTo(Octagon shape) {
        // compareTo is limited to only comparing Octagons
        // returns a 1 if this shape is bigger than the comparison shape
        // -1 if smaller, or 0 otherwise (if equal)
        if (calcArea() > shape.calcArea()) {
            return 1;
        } else if (calcArea() < shape.calcArea()) {
            return -1;
        } else {
            return 0;
        }
    }

    public static void main(String[] args) {
        //static main method to demonstrate question2.Octagon's methods
        Octagon octagon1 = new Octagon(12.5);
        Octagon octagon2 = new Octagon();

        octagon2.updateFromConsole();

        System.out.println("Octagon 1: ");
        octagon1.writeToConsole();
        System.out.println("Octagon 2: ");
        octagon2.writeToConsole();

        if(octagon1.compareTo(octagon2) == 1) {
            System.out.println("Octagon 1 is bigger!");
        } else if (octagon1.compareTo(octagon1) == -1) {
            System.out.println("Octagon 1 is smaller!");
        } else {
            System.out.println("These octagons are the same!");
        }
    }
}
