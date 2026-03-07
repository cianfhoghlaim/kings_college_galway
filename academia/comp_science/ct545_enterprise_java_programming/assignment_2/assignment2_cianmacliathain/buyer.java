import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class Buyer implements Runnable {
    private int id;
    private CarShowroom showroom;

    // AtomicIntegers ensures the static counters update atomically when accessed concurrently
    static AtomicInteger totalBuyers = new AtomicInteger(0);
    static AtomicInteger totalPurchases = new AtomicInteger(0);

    // these variables are used to store the Buyer's randomly generated preferences
    private int toSpend;
    private String preferredColour;


    Buyer(CarShowroom showroom) {
        this.showroom = showroom;

        // instantiate a random number generator to generate Buyer's preferences
        Random generator = new Random();

        // randomly generate the buyer's spending money ranging from 5000 to 24000, to increase the likelihood of
        // affording cars in our showroom
        this.toSpend = 5000 + generator.nextInt(19001);

        // randomly choose one of the statically available Car colours
        this.preferredColour = Car.colours[generator.nextInt(Car.colours.length)];
    }

    @Override
    public void run() {
        // upon running, assign a unique id to this instance of Buyer, based on how many previous Buyers instantiated
        this.id = totalBuyers.incrementAndGet();
        System.out.println( "A new buyer #" + Buyer.totalBuyers.get() + " just appeared, " +
                            "wanting to buy a car for less than €" + toSpend + ", preferably " + preferredColour + ".");

        // synchronize the showroom variable shared between other instances of Buyer and Seller
        // this avoids problems related to concurrency by limiting access to whichever instance owns the lock
        synchronized (showroom) {
            // showroom.checkStock returns the index of the first Car that matches the Buyer's preference
            // returns -1 if no match, including if showroom's ArrayList is empty
            int carToBuy = showroom.checkStock(preferredColour, toSpend);

            while (carToBuy == -1) {
                // if no match was found, output accurate message explaining why
                if (showroom.isEmpty()) {
                    System.out.println("Buyer #" + id + " is trying to buy a car but the showroom is empty.");
                } else {
                    System.out.println("Buyer #" + id + " is trying to buy a car but can't find a " +
                                        preferredColour + " car for less than €" + toSpend + '.');
                }

                // ignoring the Buyer's preferred colour, check whether there is another car they can afford
                if (showroom.checkStock(toSpend) != -1) {
                    System.out.println("Buyer #" + id + " settled for a different car that they can afford.");
                    carToBuy = showroom.checkStock(toSpend);
                    break; // break out from the while loop, as the carToBuy index is now != -1, as required
                }

                // before waiting for more cars to come to the showroom, check if it's the last day.
                // this is in the Buyer and Seller classes to avoid possible deadlocks at the end
                if (showroom.closing) {
                    System.out.println("This is the last day, Buyer #" + id + " can't wait any longer.");
                    break;
                }
                // wait before checking the showroom again for a match
                try {
                    showroom.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                carToBuy = showroom.checkStock(preferredColour, toSpend);
            }


            // may have exited the while loop due to no match and being unable to wait longer
            if (carToBuy == -1) {
                System.out.println("Buyer #" + id + " left the showroom without buying a car.");
            } else {
                // if carToBuy != -1, buy the car at the index matching the Buyer's colour / price preference
                // showroom.takeCar(carToBuy) returns the specified Car from showroom, enabling it to be printed
                System.out.println("Buyer #" + id + " bought a " + showroom.takeCar(carToBuy) + " from the showroom." +
                                    "\n" + "This is purchase #" + totalPurchases.incrementAndGet() + ".");
            }
            // notify the other waiting threads that this Buyer has finished accessing showroom
            showroom.notifyAll();
        }
    }
}




