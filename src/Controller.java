import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


public class Controller implements Runnable {
    public static final int initiatorpid = new Random().nextInt(7);
    public static final int faultypid1 = new Random().nextInt(7);
    public static int faultypid2 = -1;
    private static final int faultycount = new Random().nextInt(2) + 1;
    private Vector<Process> processVector;
    private Map<Integer, Integer> ports;
    private Map<Integer, Semaphore> semaphores;
    public static int numRounds;
    private Process[] processes;
    public Vector<Process> generals = new Vector<>();

    public void run() {
        setUpPorts();
        semaphores = new HashMap<>();
        processVector = new Vector<>();
        for (int i = 0; i < 7; i++) {
            semaphores.put(i, new Semaphore(1));
        }
        if (faultycount == 2) {
            do {
                faultypid2 = new Random().nextInt(7);
            } while (faultypid2 == faultypid1);
            numRounds = 2;
        } else numRounds = 1;
        ExecutorService ex = Executors.newFixedThreadPool(7);
        processes = new Process[7];
        for (int i = 0; i < 7; i++) {
            Process p = new Process(i, this);
            processes[i] = p;
            ex.execute(p);
        }
        ex.shutdown();

        while (processVector.size() < 7) { }
        processVector.clear();
        System.out.println("\n\nROUND 0 ----");
        processes[initiatorpid].initiate();
        while (processVector.size() < 1) { }
        processVector.clear();
        for (Process p : processes) {
            p.round0();
        }
        while (processVector.size() < 6) {}
        for (int i = 1; i <= numRounds; i++) {
            System.out.println("\n\nROUND " + i);
            round(i);
        }

    }

    public void round(int r) {
            processVector.clear();
            for (Process p : processes) {
                p.sendMsgs(r);
            }
            while (processVector.size() < 6) {
            }
            processVector.clear();
            for (Process p : processes) {
                p.receiveMsgs(r);
            }
            while (processVector.size() < 6) {
            }

    }



    public void processfinished(Process p) {
        this.processVector.add(p);
    }



    public void setUpPorts() {
        ports = new HashMap<>();
        ports.put(0, 4045);
        ports.put(1, 4046);
        ports.put(2, 4047);
        ports.put(3, 4048);
        ports.put(4, 4049);
        ports.put(5, 4050);
        ports.put(6, 4051);
    }

    public int getProcessPort(int i) {
        return ports.get(i);
    }

    public Semaphore getProcessSemaphore(int i) {
        return semaphores.get(i);
    }

    public boolean waitForProcesses(int n) {
        while (processVector.size() < n) {
            //busy wait while waiting to hear back from N processes;
        }
        return true;
    }
}
