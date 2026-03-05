import java.util.concurrent.atomic.AtomicInteger;

public class Seller implements Runnable{
    private int id;
    static AtomicInteger totalSellers = new AtomicInteger(0);
    static AtomicInteger totalSales = new AtomicInteger(0);
    private CarShowroom showroom;
    private Car sellerCar;

    Seller(CarShowroom showroom) {
        this.showroom = showroom;
        // upon Seller instantiation, a new random car is assigned to the instance variable sellerCar
        sellerCar = new Car();
    }

    @Override
    public void run() {
        // upon running, assign a unique id to this instance of Buyer, based on how many previous Buyers instantiated
        this.id = totalSellers.incrementAndGet();
        System.out.println("A new seller #" + id + " just appeared with a " + sellerCar + ".");

        // synchronize the showroom variable shared between other instances of Buyer and Seller
        // this avoids problems related to concurrency by limiting access to whichever instance owns the lock
        synchronized (showroom) {
            while (showroom.isFull()) {
                // if the showroom is full, inform the user
                System.out.println("Seller #" + id + " is trying to sell a car, but the showroom is full.");
                if (showroom.closing) { // don't wait if it's the last day that the showroom is open
                    System.out.println("This is the last day, Seller #" + id + " can't wait any longer.");
                    break;
                }
                // wait before checking if showroom is full again
                try {
                    showroom.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // while loop may have broken due to showroom closing while showroom is still full,
            // this avoids deadlock if showroom is full on last day
            if (showroom.isFull()) {
                System.out.println("Seller #" + id + " left the showroom without selling their car.");
            } else {
                showroom.addCar(sellerCar);
                System.out.println("Seller #" + id + " sold their " + sellerCar + " to the showroom." +
                                    "\n" + "This is sale #" + totalSales.incrementAndGet() + ".");
            }
            // notify the other waiting threads that this Seller has finished accessing showroom
            showroom.notifyAll();
        }
    }
}


