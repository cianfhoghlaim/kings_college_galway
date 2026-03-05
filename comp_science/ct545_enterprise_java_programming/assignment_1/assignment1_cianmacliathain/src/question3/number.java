package question3;

abstract class Number {
    // this class is abstract, so can't be instantiated itself
    // but can be extended by another class.
    // the abstract method getNumber must implemented by subclasses of question3.Number
    public abstract double getNumber();

    public static void main(String[] args) {
        // created two question3.GenericCircle objects and prints their area
        GenericCircle<NumberDouble> circle1 = new GenericCircle<>(new NumberDouble(6.9));
        GenericCircle<NumberInt> circle2 = new GenericCircle<>(new NumberInt(6));

        System.out.println("Circle 1 Area: " + circle1.calcArea());
        System.out.println("Circle 2 Area: " + circle2.calcArea());
    }
}
