public abstract class Shapes implements ShapesRelate {
    protected double area; // instance double will have a default value of 0.0

    public Shapes() {
    }


    // getArea will allow for public access of the protected double area
    public double getArea() {
        return area;
    }

    abstract void calculateArea(); //subclasses of Shapes will implement the method to calculate area

    // a 1 will be returned if this shape is bigger than the other shape
    // -1 if this shape is smaller
    // 0 if the shapes have the same area
    @Override
    public int compareShapes(ShapesRelate shape) {
        if (area - ((Shapes) shape).getArea() == 0) {
            return 0;
        } else if (area - ((Shapes) shape).getArea() > 0) {
            return 1;
        } else {
            return -1;
        }
    }
}
