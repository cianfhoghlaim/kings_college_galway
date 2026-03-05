import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Scanner;

public class Driver {
    public static void main(String[] args) {
        //create new ArrayList of Contact objects
        ArrayList<Contact> contacts = new ArrayList<>();

        //add 4 instances of Contact to the Arraylist
        contacts.add(new Contact("Cian Mac Liatháin", 01));
        contacts.add(new Contact("Rand Al'Thor", 02));
        contacts.add(new Contact("Perrin Aybara", 03));
        contacts.add(new Contact("Matrim Cauthon", 04));

        //create an iterator of the arraylist
        ListIterator<Contact> iterator = contacts.listIterator();
        //iterate forwards
        System.out.println("Output list forwards:");
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
        System.out.println("Output list backwards:");

        //iterate backwards
        while (iterator.hasPrevious()) {
            System.out.println(iterator.previous());
        }
        System.out.println("List size:" + contacts.size());

        Scanner scanner = new Scanner(System.in);

        //ask the user for a name to search
        System.out.print("Input the name of a contact to search in the list: ");
        String name = scanner.nextLine();

        //create a boolean to indicate whether name is found
        boolean nameFound = false;
        //create a variable for the index of the found name, default value of 0
        int index = 0;

        //search the arraylist for the name
        for (Contact contact: contacts) {
            if (contact.getName().equals(name)) {
                nameFound = true;
                index = contacts.indexOf(contact);
                break; //breaks the enhanced for loop if name is found
            }
        }

        //output the index if the name is found, and remove the name
        if (nameFound) {
            System.out.println("Contact found at index " + index);
            System.out.println("Removed contact at index " + index);
            contacts.remove(index);
        } else {
            System.out.println("Contact not found.");
        }

        //output the latest content of the arraylist
        System.out.println("Contents of the list:");
        for (Contact contact: contacts) {
            System.out.println(contact);
        }
    }
}
