public class Person {
    //Data Members
    private int age; // The age of this person
    private String name; // The name of this person
    private char gender; // The gender of this person

    // Default no argument constructor
    public Person() {
        this("Unassigned", 0, 'U'); // invokes overloaded constructor
    }

    public Person(String personName, int personAge, char personGender) {
        name = personName;
        age = personAge;
        gender = personGender;
    }

    // Returns the age of this person
    public int getAge() {
        return age;
    }

    // Returns the name of this person
    public String getName() {
        return name;
    }

    // Returns the gender of this person
    public char getGender() {
        return gender;
    }

    // Sets the age of this person
    public void setAge(int personAge) {
        age = personAge;
    }

    // Sets the name of this person
    public void setName(String personName) {
        name = personName;
    }

    // Sets the gender of this person
    public void setGender(char personGender) {
        gender = personGender;
    }

    @Override
    public String toString() {
        return getName() + " " + getAge() + " " + getGender();
    }
}
