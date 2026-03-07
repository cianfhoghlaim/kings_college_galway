import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class University implements Serializable {
    private String name;
    private String address;
    private List<Discipline> disciplines;

    // default constructor sets default values of instance variables
    public  University() {
        this.name = "Default Name";
        this.address = "Default Address";
        this.disciplines = new ArrayList<>();
    }

    // parameterised constructor allows for name and address to be set at instantiation
    public University(String name, String address) {
        this.name = name;
        this.address = address;
        this.disciplines = new ArrayList<>();
    }

    // getters allow for access to private variables from outside of this class
    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public List<Discipline> getDisciplineList() {
        return disciplines;
    }

    // instantiates and adds a Discipline object to the list of university's discipline
    public void addDiscipline(String name, int numEmployees) {
        disciplines.add(new Discipline(name, numEmployees));
    }

    // instantiates and returns a Discipline object
    public Discipline discipline(String name, int numEmployees) {
        return new Discipline(name, numEmployees);
    }

    @Override
    public String toString() {

        return "University Name: " + name + '\n' +
                "University Address: " + address + '\n' +
                "Disciplines: " + '\n' +
                disciplines;
    }

    public class Discipline implements Serializable{
        private String name;
        private int numEmployees;

        // default constructor sets default values of instance variables
        public Discipline() {
            this.name = "Default Name";
            this.numEmployees = 0;
        }

        // parameterised constructor allows for name and address to be set at instantiation
        public Discipline(String name, int numEmployees) {
            this.name = name;
            this.numEmployees = numEmployees;
        }

        // getters allow for access to private variables from outside of this class
        public String getName() {
            return name;
        }

        public int getNumEmployees() {
            return numEmployees;
        }

        @Override
        public String toString() {
            return "Discipline Name: " + name + ',' + '\n'
                            + name + " Employees: " + numEmployees;
        }
    }
}


