package question3;

//code for GenericCircle adapted from Circle in Lecture 1B
// GenericCircle is a subclass of GeometricFigure3
// Type Bounds used to make GenericCircle a generic class with a type parameter
// limited to only subclasses of number
public class GenericCircle<T extends Number> extends GeometricFigure3 {
    // the type of variable radius depends on how GenericCircle is instantiated
    private T radius;

    public GenericCircle(T radius) { this.radius = radius;
    }

    public NumberDouble calcArea() {
        return new NumberDouble(radius.getNumber() * radius.getNumber() * PI);
    }

    public void display() {
        System.out.println("This is a circle with radius " + radius);
    }
}