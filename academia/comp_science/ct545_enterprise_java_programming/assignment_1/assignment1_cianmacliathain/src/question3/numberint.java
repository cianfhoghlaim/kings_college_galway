package question3;

public class NumberInt extends Number{
    private int number;

    public NumberInt(int number) {
        this.number = number;
    }

    @Override
    public double getNumber() {
        return number;
    }
}
