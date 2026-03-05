public class Movie {
    // these private variables can only be accessed through the publicly available getters/setters
    private String title;
    private String genre;
    private int rating;

    // created overloaded constructors
    public Movie() {
        this.title = "Unknown";
        this.genre = "Unknown";
        this.rating = 0;
    }

    public Movie(String title, String genre, int rating) {
        this.title = title;
        this.genre = genre;
        this.rating = rating;
    }

    public void playIt() {
        System.out.println(title + " - Now playing");
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    @Override
    // output object relevant data as String
    public String toString() {
        return "Title: " + title + '\n' +
                "Genre:" + genre + '\n' +
                "Rating:" + rating;
    }
}
