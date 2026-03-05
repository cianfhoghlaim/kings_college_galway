import java.util.Random;

public class Car {
    static final String[] colours = {"Black", "Blue", "Red", "Silver", "White"};
    static final String[] counties = {"C", "CE", "CN", "CW", "D", "DL", "G", "KE", "KK", "KY", "L", "LD", "LH", "LK",
            "LM", "LS", "MH", "MN", "MO", "OY", "RN", "SO", "T", "W", "WH", "WX", "WW"};
    private String registration;
    private int value;
    private String colour;

    Car() {
        Random generator = new Random();

        // this will randomly generate a value between 1000 and 20,000
        this.value = 1000 + generator.nextInt(19001);

        // randomly select a colour from the statically available array of choices
        this.colour = colours[generator.nextInt(colours.length)];

        // randomly choose a year from 00 - 2022, and format it with double digits in the case of 1-9
        String registrationYear = String.format("%02d", generator.nextInt(22));

        // randomly select a county from the statically available array of choices
        String registrationCounty = counties[generator.nextInt(counties.length)];

        //if the registration number generated is less than 4 digits (e.g. 22) then add leading 0's until it is the
        // correct length of 4 (e.g. 0022)
        String registrationNumber = String.format("%04d", generator.nextInt(9999));

        // combine the components of the Car's registration
        this.registration = registrationYear + '-' + registrationCounty + '-' + registrationNumber;
    }

    // this getter and setter will be used by the CarShowroom to add a markup to cars
    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    // this getter will be used by the CarShowroom to compare the Car's colour to the Buyer's preference
    public String getColour() {
        return this.colour;
    }

    @Override
    public String toString() {
        return this.colour + " car with registration " + this.registration + " worth €" + this.value;
    }

    public static void main(String[] args) {
        Car testCar = new Car();
        System.out.println(testCar);
    }
}
