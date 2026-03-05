import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Driver {
    public static void main(String[] args) {

        ArrayList<Shapes> shapes = new ArrayList<>();

        shapes.add(new Circle(1));
        shapes.add(new Circle(5));
        shapes.add(new Circle(3));
        shapes.add(new Rectangle(5, 7));
        shapes.add(new Rectangle(2, 3));
        shapes.add(new Rectangle(1, 2));

        for (Shapes shape: shapes) {
            shape.calculateArea();
        }

        Shapes largest = largestShape(shapes);
        System.out.println(largest);
        System.out.println("Index: " + shapes.indexOf(largest));
    }
    
    public static Shapes largestShape(List<Shapes> list) {

        Iterator<Shapes> iterator = list.iterator();
        Shapes largest = iterator.next();

        while (iterator.hasNext()) {
            Shapes nextShape = iterator.next();
            if (largest.compareShapes(nextShape) == - 1) {
                largest = nextShape;
            }
        }
        return largest;
    }
}
