import java.util.Scanner;

public class Tester_2B {
    public static void main(String[] args) {
        Movie[] movies = new Movie[3]; //create an array of Movies of length 3
        Scanner scanner = new Scanner(System.in); // create scanner for user input

        for (int i = 0; i < movies.length; i++) {
            Movie movie = new Movie(); //use default constructor before asking for user input
            movies[i] = movie;
            System.out.print("Please enter the title of movie " + (i + 1) +": ");
            movies[i].setTitle(scanner.nextLine());

            System.out.print("Please enter the genre of movie " + (i + 1) +": ");
            movies[i].setGenre(scanner.nextLine());

            System.out.print("Please enter the rating (1-5) of movie " + (i + 1) +": ");
            movies[i].setRating(scanner.nextInt());
            scanner.nextLine(); //use .nextLine() after .nextInt() to account for the \n from user's input

        }

        // display the title, genre, rating and playIt() output for each movie in the array
        for (int i = 0; i < movies.length; i++) {
            System.out.println("Movie " + (i + 1) + " Title: " + movies[i].getTitle());
            System.out.println("Movie " + (i + 1) + " Genre: " + movies[i].getGenre());
            System.out.println("Movie " + (i + 1) + " Rating: " + movies[i].getRating());
            movies[i].playIt();
        }
    }
}
