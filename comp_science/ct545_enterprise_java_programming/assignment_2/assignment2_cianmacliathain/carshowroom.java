import java.util.ArrayList;

public class CarShowroom {
    // volatile to ensure that each thread doesn't cache its own value and miss when it is changed
    public volatile boolean closing; // instance boolean automatically gets assigned default value of false

    private int capacity;
    private ArrayList<Car> cars;

    CarShowroom(int capacity) {
        this.capacity = capacity; // set the capacity based on the constructor's parameter
        cars = new ArrayList<>(); // instantiate the showroom's ArrayList
    }

    public boolean isEmpty() {
        return cars.isEmpty();
    }

    public boolean isFull() {
        return cars.size() == capacity;
    }

    public void addCar(Car newCar) {
        if (!isFull()) { // only adds a new car if the list size is below capacity
            newCar.setValue((int) (newCar.getValue() * 1.20)); // adds a 20% mark to the price paid to the Seller
            cars.add(newCar);
        }
    }

    // overloaded methods used to find cars matching Buyers' preferences
    public int checkStock(String preferredColour, int toSpend) {
        for (Car car : cars) {
            if (car.getColour().equals(preferredColour) && car.getValue() <= toSpend) {
                return cars.indexOf(car);
            }
        }
        return -1;
    }

    public int checkStock(int toSpend) {
        for (Car car : cars) {
            if (car.getValue() <= toSpend) {
                return cars.indexOf(car);
            }
        }
        return -1;
    }

    // used to indicate to Buyer and Seller class that the CarShowroom is closing and they cannot wait further
    public void closeShowroom() {
        this.closing = true;
    }

    // overloaded methods to remove a Car from the CarShowroom

    // this method was used in the original Assignment answer
    // before adding the additional requirement of Buyer spending power
    public Car takeCar() {
        return cars.remove(0); // returns and removes the first (oldest) car in the list
    }

    public Car takeCar(int index) {
        return cars.remove(index); //returns and removes the car at the specified index
    }

    // used to print all Cars in the CarShowroom
    public void showStock() {
        for (int i = 0; i < cars.size(); i++) {
            System.out.println("Car " + i + ": " + cars.get(i));
        }
    }
}
