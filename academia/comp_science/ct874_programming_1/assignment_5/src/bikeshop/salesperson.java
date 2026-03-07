//Assignment 5 classes are all contained in the package bikeshop
package bikeshop;

public class SalesPerson extends SalesEmployee{
    public SalesPerson() {
        super(); //uses superclass SalesEmployee's constructor
    }

    public SalesPerson(String firstName, String lastName, String ppsNumber) {
        super(firstName, lastName, ppsNumber);
    }

    @Override
    void calculateCommission() {
        this.commission = sales/100*15; //commission for each SalesPerson is calculated at 15%
    }
}
