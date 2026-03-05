import java.util.Random;

public class Main {
    public static void main(String[] args) {
        // final variables help other developers understand that these variables aren't later altered by the code
        // allows for easy changing of scenario
        final int showroomCapacity = 10;
        final int daysOpen = 30;
        final int maxDailySellers = 4;
        final int maxDailyBuyers = 4;

        CarShowroom showroom = new CarShowroom(showroomCapacity);

        // this random number generator will be used to randomly generate the daily Sellers and Buyers
        Random generator = new Random();

        for (int i = 1; i <= daysOpen; i++) {
            // Output the day and current number of cars in the showroom
            System.out.println("Day " + i + " beginning. There are "
                    + (Seller.totalSales.get() - Buyer.totalPurchases.get())
                            + " cars in the " + "showroom today.");

            // on the last day, toggle the showroom's boolean to inform threads that they can't wait further.
            if (i == daysOpen) {
                showroom.closeShowroom();
                System.out.println("The showroom is getting ready to close.");
            }

            // these Seller and Buyer threads don't need to be referred to later, so are instantiated anonymously
            for (int j = 0; j < generator.nextInt(maxDailySellers); j++) {
                new Thread(new Seller(showroom)).start();
            }

            for (int j = 0; j < generator.nextInt(maxDailyBuyers); j++) {
                new Thread(new Buyer(showroom)).start();
            }

            // short delay at the end of each day
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            System.out.println(""); // add a line break after each day for easier output reading
        }

        // summarise the activity of the showroom
        System.out.println("Day " + daysOpen + " is over. The showroom is now closed to visitors.");
        System.out.println("Total Sales: " + Seller.totalSales);
        System.out.println("Total Purchases: " + Buyer.totalPurchases);
        System.out.println("Stock Remaining: ");
        showroom.showStock();
    }
}

