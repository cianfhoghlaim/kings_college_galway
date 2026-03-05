//CT874 Assignment 5
//Name: Cian Mac Liatháin
//ID: 11476078
//Assignment 5 classes are all contained in the package bikeshop
package bikeshop;

import java.util.ArrayList;
import java.util.Scanner;

public class Driver {
    public static void main(String[] args) {
        //Scanner to allow for user input
        Scanner scanner = new Scanner(System.in);

        //Instantiate an ArrayList of objects of type SalesEmployee
        ArrayList<SalesEmployee> employees = new ArrayList<>();

        //two different methods of instantiating SalesEmployee objects
        SalesAgent agent1 = new SalesAgent(); //default constructor
        SalesEmployee agent2 = new SalesPerson("Cian", "Mac Liatháin", "1234567U");
        agent2.sales = 37650; //set the protected variable, accessible as sharing bikeshop package

        // subclass objects added to arraylist
        employees.add(agent1);
        employees.add(agent2);

        // emulate an application menu
        int choice;
        boolean exit = false;

        while (!exit) {
            System.out.println();
            System.out.println("***** ACME Bike Shop Sales Records *****");
            System.out.println("1. View Employees");
            System.out.println("2. Add Sales Agent");
            System.out.println("3. Add Sales Employee");
            System.out.println("4. Update Sales Figures");
            System.out.println("0. Exit");
            System.out.print("Please select an option from above: ");

            try {
                choice = scanner.nextInt();
                scanner.nextLine();
            } catch (Exception e) {
                scanner.nextLine(); //consume token left behind from invalid input to nextInt();
                System.out.println("Incorrect input format, please enter the number of a menu option.");
                System.out.println();
                continue;
            }

            //switch statement based on user input

            switch (choice) {
                case 0: // exit menu
                    System.out.println("Exiting application... slán!");
                    exit = true;
                    break;
                case 1: // list all employees in ArrayList
                    System.out.println("\t" + "1. View Employees");
                    System.out.println(employees.size() + " employees registered: ");
                    for (SalesEmployee employee: employees) {
                        System.out.println();
                        System.out.println(employee.toString());
                    }
                    break;

                case 2: // add sales agent
                    SalesAgent newAgent = new SalesAgent();
                    System.out.println("\t" + "2. Add Sales Agent");
                    System.out.print("What is the employee's first name? ");
                    newAgent.setFirstName(scanner.nextLine());

                    System.out.print("What is the employee's last name? ");
                    newAgent.setLastName(scanner.nextLine());

                    System.out.print("What is the employee's PPS number? ");
                    newAgent.setPPS(scanner.nextLine());

                    System.out.print("What is the employee's total sales? ");

                    try {
                        newAgent.sales = scanner.nextDouble();
                    } catch (Exception e) {
                        scanner.nextLine();
                        System.out.println("Incorrect format entered, please try again.");
                        break;
                    }
                    employees.add(newAgent);
                    System.out.println("Employee added.");
                    break;

                case 3: // add sales person
                    SalesPerson newPerson = new SalesPerson();
                    System.out.println("\t" + "3. Add Sales Person");
                    System.out.print("What is the employee's first name? ");
                    newPerson.setFirstName(scanner.nextLine());

                    System.out.print("What is the employee's last name? ");
                    newPerson.setLastName(scanner.nextLine());

                    System.out.print("What is the employee's PPS number? ");
                    newPerson.setPPS(scanner.nextLine());

                    System.out.print("What is the employee's total sales? ");

                    try {
                        newPerson.sales = scanner.nextDouble();
                    } catch (Exception e) {
                        scanner.nextLine();
                        System.out.println("Incorrect format entered, please try again.");
                        continue;
                    }
                    employees.add(newPerson);
                    System.out.println("Employee added.");
                    break;


                case 4: // allow user to update sales of any person SalesEmployee in ArrayList
                    System.out.println("\t" + "4. Update Sales Figures");
                    System.out.print("Enter the Employee Number of the employee you'd like to update: ");
                    choice = scanner.nextInt();

                    for (int i = 0; i < employees.size(); i++) {
                        if (employees.get(i).getEmployeeNumber() == choice) {
                            System.out.println("Employee found: " + '\n');
                            System.out.println(employees.get(i).toString());
                            System.out.print("What are the employee's latest sales figures? ");
                            try {
                                employees.get(i).sales = scanner.nextDouble();
                            } catch (Exception e) {
                                scanner.nextLine();
                                System.out.println("Incorrect format entered, please try again.");
                                continue;
                            }
                            System.out.println("Sales figures updated!");
                        } else {
                            System.out.println("Employee not found.");
                        }
                    }
                    break;
                default: // if the number entered isn't 0-4
                    System.out.println("Incorrect option selected, please enter the number of a menu option.");
                    System.out.println();
            }
        }
    }
}
