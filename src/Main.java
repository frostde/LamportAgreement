/*Daniel Frost
* Eliza Karki
* Project 3 Lamport Agreement Algorithm*/


public class Main {

    public static void main(String[] args) {
        Controller c = new Controller();

        Thread t = new Thread(c);
        t.start();
    }
}
