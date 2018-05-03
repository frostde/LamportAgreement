
public class Main {

    public static void main(String[] args) {
        Controller c = new Controller();

        Thread t = new Thread(c);
        t.start();
    }
}
