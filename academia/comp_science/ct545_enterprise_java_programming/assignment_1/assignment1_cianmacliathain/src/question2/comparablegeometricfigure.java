package question2;

// the use of Generics and Type Bounds allows for this comparable interface
// to be limited to subclasses of GeometricFigure2
public interface ComparableGeometricFigure<T extends GeometricFigure2> extends Comparable<T>{
    int compareTo(T shape);
}
