package question1;

class Workaholic extends Worker {
    // Both Workaholic and Worker share name, age, earned and hourlyIncome variables
    // these can be declared in the Worker class and inherited by the Workaholic class
    // In this assignment these variables are declared as private, and it is unclear whether
    // this private secure is an intended feature of the Worker class or whether they may be changed to protected
    // I've chosen to use getters and setters to allow Workaholic to access these
    // In future, if classes are developed further, the use of setters may be beneficial in how
    // they can be used to limit the changes that can be made e.g. don't allow hourlyIncome over a certain amount
    // but this is outside the scope of this assignment
    public static final int OVERTIME = 500;


    public Workaholic(String name, float hourlyIncome, int age) {
        // a workaholic has no coworker, and can make call the constructor of Worker with coWorker = null
        super(name, hourlyIncome, age, null);
    }

    public void work(int hours) {
        // previously two for loops were used
        // one added hourlyIncome to earned for each hour
        // the other added hourlyIncome to earned for every hour OVERTIME, regardless of hours worked
        // this is simplified here by first calculating totalHours = hours + OVERTIME, multiplying this by
        // hourlyIncome, adding this to what had been earned already and then settings earned to this new value
        setEarned(getEarned() + getHourlyIncome() * (hours + OVERTIME));
    }

    public void work() {
        int yearsWorking = RETIREMENT_AGE - getAge();
        while (yearsWorking-- > 0) work(2000);
    }
}
