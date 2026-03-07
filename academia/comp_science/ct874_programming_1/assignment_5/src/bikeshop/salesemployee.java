//Assignment 5 classes are all contained in the package bikeshop
package bikeshop;

//abstract class allows for implementation of functionality to be implemented / overrided by subclasses
public abstract class SalesEmployee {
    // static variable accessible by all instances of SalesEmployee
    private static int bikeEmployeeNumber;

    // private variables only access
    private String firstName;
    private String lastName;
    private String ppsNumber;
    private int employeeNumber;

    protected double sales = 0; // defaults to 0 until sales specified by user
    protected double commission;

    //overloaded constructors can be used by subclasses
    public SalesEmployee() {
        // constructor using default values if user provides no arguments
        bikeEmployeeNumber+=1; // increment static variable before assigning to instance's employeeNumber
        this.employeeNumber = bikeEmployeeNumber; //first employee has employeeNumber 1
        this.firstName = "Name";
        this.lastName = "Unknown";
        this.ppsNumber = "0000000Z";
    }

    public SalesEmployee(String firstName, String lastName, String ppsNumber) {
        bikeEmployeeNumber+=1; // increment static variable before assigning to instance's employeeNumber
        this.employeeNumber = bikeEmployeeNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.ppsNumber = ppsNumber;
    }

    // Public Getters and Setters allow for the access / change of the private variables
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPPS(String ppsNumber) {
        this.ppsNumber = ppsNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPPS() {
        return ppsNumber;
    }

    public int getEmployeeNumber() {
        return employeeNumber;
    }

    public String toString() {
        calculateCommission();
        return "Employee Number: " + this.employeeNumber + "\n" +
                "Name: " + this.firstName + " " + this.lastName + "\n" +
                "PPS Number: " + this.ppsNumber + "\n" +
                "Total sales: " + this.sales + "\n" +
                "Commission paid: " + this.commission;
    }

    // abstract method must be implemented by any subclass of SalesEmployee
    abstract void calculateCommission();

}
