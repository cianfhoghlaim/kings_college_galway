public class Contact {
    private String name;
    private long number;

    //create constructor for class
    Contact(String name, long number) {
        this.name = name;
        this.number = number;
    }

    //create publicly accessible getters and setters to allow for
    // access to and changing of private variables

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", Number: " + number;
    }
}
