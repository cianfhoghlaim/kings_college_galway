//Assignment 5 classes are all contained in the package bikeshop
package bikeshop;

public class SalesAgent extends SalesEmployee{
    public SalesAgent() {
        super(); //uses superclass SalesEmployee's constructor
    }

    public SalesAgent(String firstName, String lastName, String ppsNumber) {
        super(firstName, lastName, ppsNumber);
    }

    @Override
    void calculateCommission() {
        this.commission = sales/100*10; //commission for each SalesAgent is calculated at 10%
    }
}
