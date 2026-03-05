public class Tester {
    public static void main(String[] args) {
        //create a new array of size 4 of Student objects
        Student[] students = new Student[4];

        //create instances of student and add to the array
        students[0] = new Student("Cian Mac Liatháin", 11476078);
        students[1] = new Student("Banba", 12587189);
        students[2] = new Student("Ériu", 20698290);

        students[3] = new Student();
        students[3].setName("Fódla");
        students[3].setIDnumber(21799391);

        //output content of array
        for (int i = 0; i < students.length; i++) {
            System.out.println(students[i].toString());
        }
    }
}
