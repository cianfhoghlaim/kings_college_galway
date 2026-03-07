package question1;

class Worker {
        public static final int RETIREMENT_AGE = 65;
        // instance variables will be assigned default values upon question1.Worker class (or subclass) instantiation
        // so do not need to be declared here
        private String name;
        private int age;
        private float earned;
        private float hourlyIncome;
        private Worker coWorker;

        public Worker(String name, float hourlyIncome, int age, Worker coWorker) {
            this.name = name;
            this.hourlyIncome = hourlyIncome;
            this.age = age;
            this.coWorker = coWorker;
        }

        public void work(int hours) {
            // simplify for loop by calculating total income for all hours and adding it to earned
            earned += hourlyIncome * hours;

            // continues to delegate 1 hour for every 5 worked
            if (coWorker != null && hours % 5 == 0) delegate(hours/5);
            }


        public void work() {
            while (age++ < RETIREMENT_AGE) work(1600);
        }

        private void delegate(int hours) {
            coWorker.work(hours);
        }


        // Setters and Getters required by the Workaholic class
        // unnecessary getters and setters were not created in order to maintain the private security
        // of the original classes, but these can be added in future when further functionality is required

        public int getAge() {
            return age;
        }

        public float getEarned() {
            return earned;
        }

        public void setEarned(float earned) {
            this.earned = earned;
        }

        public float getHourlyIncome() {
            return hourlyIncome;
        }

        public String info() {
            return name + " earned " + earned;
        }

    public static void main(String[] args) {
        Worker jane = new Worker("Jane", 20, 25, null);
        Worker john = new Worker("John", 20, 45, jane);

        Workaholic bill = new Workaholic("Bill", 20, 25);
        john.work();
        jane.work();
        bill.work();
        System.out.println(john.info());
        System.out.println(jane.info());
        System.out.println(bill.info());
    }
}
