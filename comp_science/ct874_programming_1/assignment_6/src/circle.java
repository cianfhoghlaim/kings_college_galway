public class Circle extends Shapes{
    private double radius; // instance doubles have a default value of 0.0
    private double PI = Math.PI;

    //Java compiler will not add a default constructor if a parameterised constructor is specified
    public Circle() {
    }

    //Parameterised constructor allows for creation of Circle objects with a specific radius
    public Circle(double radius) {
        this.radius = radius;
    }

    // Getters and Setters allow for public access of the private double radius
    public void setRadius (double radius) {
        this.radius = radius;
    }

    public double getRadius() {
        return radius;
    }


    //Update the Circle's area with the latest values for length and width
    @Override
    public void calculateArea() {
        this.area = radius * radius * PI; // the double area is inherited from Shapes
    }

    @Override
    public String toString() {
        return "Radius: " + radius + "\n" +
                "Area: " + area;
    }

}
