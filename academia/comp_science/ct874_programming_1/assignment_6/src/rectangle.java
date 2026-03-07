public class Rectangle extends Shapes {
    private double length; //instance doubles will default to 0.0
    private double width;

    //Java compiler will not add a default constructor if a parameterised constructor is specified
    public Rectangle() {
    }

    //Parameterised constructor allows for creation of Rectangle objects with a specific length and width
    public Rectangle(double length, double width) {
        this.length = length;
        this.width = width;
    }

    //Getters and Setters allow for public access of the private doubles length and width
    public void setWidth(double width) {
        this.width = width;
    }

    public double getWidth() {
        return width;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getLength() {
        return length;
    }

    //Update the Rectangle's area with the latest values for length and width
    public void calculateArea() {
        this.area = length * width; //the double area is inherited from the abstract class Shapes
    }

    @Override
    public String toString() {
        return "Length: " + length + "\n" +
                "Width:" + width + "\n" +
                "Area: " + area;
    }
}
