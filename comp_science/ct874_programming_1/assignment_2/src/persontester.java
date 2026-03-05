public class PersonTester {
    public static void main(String[] args) {
        Person[] people = new Person[2]; //declare an array of Person of length 2

        //instantiate two Person objects and add to array
        people[0] = new Person("Cian Mac Liatháin", 28, 'M');
        people[1] = new Person("Moiraine Damodred", 42, 'F');

        //use the toString method to summarise each Person in array's data as String
        for (int i = 0; i < people.length; i++) {
            System.out.println(people[i].toString());
        }

        //user setter to change age of the Person objects
        people[0].setAge(29);
        people[1].setAge(43);

        for (int i = 0; i < people.length; i++) {
            System.out.println(people[i].toString());
        }
    }
}
