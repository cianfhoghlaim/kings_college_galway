public class Student {
    private String name;
    private long IDnumber;

    public Student() {
        this.name = "Rúnda";
        this.IDnumber = 0;
    }

    public Student(String name, long ID) {
        this.name = name;
        this.IDnumber = ID;
    }


    //public getter and setter methods to allow for the changing of private variables
    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getIDnumber() {
        return this.IDnumber;
    }

    public void setIDnumber(long ID) {
        this.IDnumber = ID;
    }

    //toString method to allow for object serialisation
    public String toString() {
        return "Name: " + getName() + " ID: " + getIDnumber();
    }


}
