//CT874 Assignment 7
//Name: Cian Mac Liatháin
//ID: 11476078


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

public class Driver {
    public static void main(String[] args) {
        //An arraylist is created to hold University objects
        List<University> universities = new ArrayList<>();

        //universities are added to the ArrayList<University> using the parameterised constructor
        universities.add(new University("Hogwarts University for Wicthcraft and Wizardry", "123, Privet Drive"));
        universities.add(new University("Ollscoil na hÉireann, Gaillimh", "University Road, Galway, Ireland"));
        universities.add(new University("L'Université de la Magie, Paris", "La Tour Eiffel, Paris, France"));

        //disciplines are added to each university's ArrayList<Discipline>
        for (int i = 0; i < 3; i++) {

            University university = universities.get(i);

            //using the parameterised constructor addDiscipline() method

            university.addDiscipline("Enchanting", (i+1)*3);
            university.addDiscipline("Herbology", (i+1)*2);
            university.addDiscipline("Defence Against the Dark Arts", (i+1)*4);

            //instantiating and adding a Discipline object directly to the ArrayList<Discipline>
            university.getDisciplineList().add(university.discipline("Muggology", (i+4)*(i+2)));
        }

        for (University university: universities) {
            System.out.println("University " + (universities.indexOf(university) + 1) + ": " + '\n' + university);
            //the toString() method of the University class calls the toString() method of the ArrayList class,
            // which calls the toString() method of each Discipline object found in each university's
            // Arraylist of disciplines
        }


        try {
            // create a connection stream to specified file output
            FileOutputStream fileStream = new FileOutputStream("UniversityInfo.dat");

            // create a chain stream to FileOutputStream
            ObjectOutputStream os = new ObjectOutputStream(fileStream);

            // write object
            os.writeObject(universities);

            // close chain stream
            os.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        try{
            // create a connection stream to specified file input

            FileInputStream fileStream = new FileInputStream("UniversityInfo.dat");

            // create a chain stream to FileInputStream
            ObjectInputStream os = new ObjectInputStream(fileStream);

            // Read and cast object
            List<University> deserialisedUniversities  = (List<University>) os.readObject();

            // display the contents of the list
            for (University university: deserialisedUniversities) {
                System.out.println("University " + (deserialisedUniversities.indexOf(university) + 1) + ": " + '\n' + university);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
