package question3;

public class NumberDouble extends Number{
    private double number;

    public NumberDouble(double number) {
        this.number = number;
    }

    @Override
    public double getNumber() {
        return number;
    }
}
