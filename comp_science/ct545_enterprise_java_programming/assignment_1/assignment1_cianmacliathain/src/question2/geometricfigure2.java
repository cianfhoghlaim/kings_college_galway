package question2;

//code for GeometricFigure2 adapted from Lecture 1B
abstract class GeometricFigure2 {
    public static final double PI = 3.141592653;
    private boolean filled;

    public GeometricFigure2() {
        filled = false;
    }

    public boolean isFilled() {
        return filled;
    }

    public void setFilled(boolean filled) {
        this.filled = filled;
    }

    public void display() {
        System.out.println("This is some geometric figure.");
    }

    public abstract double calcArea();
}