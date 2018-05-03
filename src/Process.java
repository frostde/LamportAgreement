import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Semaphore;


public class Process implements Runnable {
    public int pid;
    private boolean isInitiator;
    private boolean isFaulty;
    private Controller controller;
    public MessageTree tree;
    private int value;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Map<Integer, Socket> sockets;
    private int initiatorpid;
    private Vector<Message> newMessages = new Vector<>();

    Process(int p, Controller c) {
        this.pid = p;
        this.controller = c;
        if (controller.initiatorpid==this.pid) isInitiator = true;
        if (controller.faultypid1==this.pid || controller.faultypid2==this.pid) isFaulty = true;
        tree = new MessageTree(this.pid);
        this.sockets = new HashMap<>();

    }

    public boolean isInitiator() {
        return isInitiator;
    }

    public boolean isFaulty() {
        return isFaulty;
    }

    public void run() {
        System.out.println(identify());
        setupSockets();
    }

    public void initiate() {
        try {
            if (isInitiator) {
                Semaphore processSemaphore = controller.getProcessSemaphore(pid);
                processSemaphore.acquire();
                int value = new Random().nextInt(2);
                System.out.println(controller.faultypid1 + " " + controller.faultypid2);
                Message m = new Message(value);
                m.path.add(pid);
                for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                    writer = new BufferedWriter(new OutputStreamWriter(entry.getValue().getOutputStream()));
                    if (entry.getKey() == controller.faultypid1 || entry.getKey() == controller.faultypid2) {
                        Message f = new Message(-1);
                        f.path.add(pid);
                        writer.write(f.toString() + "\n");
                        System.out.println(pid + " wrote " + f.toString() + " to " + entry.getKey());

                    } else {
                        writer.write(m.toString() + "\n");
                        System.out.println(pid + " wrote " + m.toString() + " to " + entry.getKey());
                    }
                    writer.flush();
                }
            }
            controller.processfinished(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void round0() {
        try {
            if (!isInitiator) {
                BufferedReader reader;
                for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                    if (controller.getProcessSemaphore(entry.getKey()).availablePermits() == 0) {
                        initiatorpid = entry.getKey();
                        reader = new BufferedReader(new InputStreamReader(entry.getValue().getInputStream()));
                        String x = reader.readLine();
                        Message m = new Message(Integer.parseInt(x.substring(x.indexOf('{')+1, x.indexOf(','))));
                        m.path.add(initiatorpid);
                        Vector<Message> v = new Vector<>();
                        v.add(m);
                        tree.insert(v, 0);
                        Vector<Message> t = new Vector<>();
                        Message p = new Message(m);
                        t.add(p);
                        p.path.add(pid);
                        tree.insert(t, 1);
                        newMessages.add(m);
                    }
                }
                controller.processfinished(this);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendMsgs(int round) {
        try {
            StringBuilder sb = new StringBuilder();
            System.out.println(newMessages.size());
            for (Message m : newMessages) {
                m.path.add(pid);
                sb.append(m.toString() + "\n");
            }
            for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                if (entry.getKey() != initiatorpid) {
                    writer = new BufferedWriter(new OutputStreamWriter(entry.getValue().getOutputStream()));
                    writer.write(sb.toString());
                    writer.flush();
                }
            }
            newMessages.clear();
            controller.processfinished(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receiveMsgs(int round) {
        //receive all messages and insert them into our tree
        try {
            BufferedReader READER;
            DataInputStream is = null;
            DataInputStream stdIn = new DataInputStream(System.in);
            if (!isInitiator) {
                for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                    if (entry.getKey() != initiatorpid) {
                        READER = new BufferedReader(new InputStreamReader(entry.getValue().getInputStream()));
                        is = new DataInputStream(entry.getValue().getInputStream());
                        if (round == 1) {
                            String x = READER.readLine();
                            Message n = new Message(Integer.parseInt(x.substring(x.indexOf('{')+1, x.indexOf(','))));
                            String path = x.substring(x.indexOf('[')+1, x.indexOf(']'));
                            path = path.replaceAll("\\D+","");
                            for (char c : path.toCharArray()) {
                                n.path.add(Integer.parseInt(Character.toString(c)));
                            }
                            newMessages.add(n);
                        } else {
                            int c;
                            StringBuilder sb = new StringBuilder();
                            while (is.available() != 0) {
                                c = is.read();
                                sb.append((char)c);
                            }
                            System.out.println("Process " + pid + " from " + entry.getKey());
                            for (String s : sb.toString().split("\n") ) {
                                Message n = new Message(Integer.parseInt(s.substring(s.indexOf('{')+1, s.indexOf(','))));
                                String path = s.substring(s.indexOf('[')+1, s.indexOf(']'));
                                path = path.replaceAll("\\D+","");
                                for (char x : path.toCharArray()) {
                                    n.path.add(Integer.parseInt(Character.toString(x)));
                                }
                                newMessages.add(n);
                            }
                        }
                    }
                }
                tree.insert(newMessages, round);
                System.out.println("\n");
            }
            controller.processfinished(this);
            System.out.println("Process " + pid + ":");
            for (MessageNode n : tree.root.children) {
                System.out.println(n.message.toString());
            }
            System.out.println(tree.root.children.size());
            System.out.println("\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    /*public void initiate() {
        try {
            if (isInitiator) {
                Semaphore processSemaphore = controller.getProcessSemaphore(pid);
                processSemaphore.acquire();
                int value = new Random().nextInt(2);
                Message m;
                for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                    writer = new BufferedWriter(new OutputStreamWriter(entry.getValue().getOutputStream()));
                    if (isFaulty) {
                        m = new Message(new Random().nextInt(2));
                        m.path.add(pid);
                        writer.write(m.toString() + "\n");
                    } else {
                        m = new Message(value);
                        m.path.add(pid);
                        writer.write(m.toString() + "\n");
                    }
                    writer.flush();
                    System.out.println("Process " + pid + " is sending " + m.toString() + " to Process " + entry.getKey());
                }
                controller.processfinished(this);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void round0() {
        try {
            if (!isInitiator) {
                BufferedReader reader;
                for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                    if (controller.getProcessSemaphore(entry.getKey()).availablePermits() == 0) {
                        initiatorpid = entry.getKey();
                        reader = new BufferedReader(new InputStreamReader(entry.getValue().getInputStream()));
                        String in = reader.readLine();
                        int value = Integer.parseInt(in.substring(in.indexOf("{")+1,in.indexOf(",")));
                        //int path = Integer.parseInt(in.substring(in.indexOf("[")+1,in.indexOf("]")));
                        Message m = new Message(value);
                        m.path.add(initiatorpid);
                        tree.insert(m, 0);
                        m.path.add(pid);
                        tree.insert(m,1);
                        //if (faulty) values[name] = (values[indexOfInitiator] == 0) ? 1 : 0;
                        //else values[name] = values[indexOfInitiator];
                    }
                }
                controller.processfinished(this);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void sendMsg(int round) throws IOException {
        if (!isInitiator) {
            for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                if (entry.getKey() != controller.initiatorpid && entry.getKey() != this.pid) {
                    writer = new BufferedWriter(new OutputStreamWriter(entry.getValue().getOutputStream()));
                    if (round == 1) {
                        writer.write(tree.root.message.toString()  + "\n");
                        System.out.println("Process " + pid + " is sending " + tree.root.message.toString() + " to Process " + entry.getKey());
                    } else {
                        for (Message m : newMessages) {
                            writer.write(m.toString() + "\n");
                            System.out.println("Process " + pid + " is sending " + m.toString() + " to Process " + entry.getKey());
                        }
                    }
                    writer.flush();
                }
            }
            System.out.println("\n");
            controller.processfinished(this);
        }
    }

    public void receiveMsg(int round) throws IOException {
        if (!isInitiator) {
            for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                if (entry.getKey() != initiatorpid) {
                    reader = new BufferedReader(new InputStreamReader(entry.getValue().getInputStream()));
                    String in = reader.readLine();
                    int value = Integer.parseInt(in.substring(in.indexOf("{")+1,in.indexOf(",")));
                    String path = in.substring(in.indexOf("[")+1,in.indexOf("]"));
                    Message m = new Message(value);
                    path = path.replaceAll("\\D+","");
                    for (char x : path.toCharArray()) {
                        m.path.add(Character.getNumericValue(x));
                    }
                    m.path.add(pid);
                    tree.insert(m, round);
                    newMessages.add(m);
                    //System.out.println("Process " + pid + " received a value of " + m.toString() + " from Process " + entry.getKey());
                }
            }
            controller.processfinished(this);
            System.out.println(newMessages.size());
        }
    }*/

    private String identify() {
        if (isInitiator) {
            if (isFaulty) {
                return "Process " + pid + " [initiator + faulty]";
            } else {
                return "Process " + pid + " [initiator]";
            }
        } else {
            if (isFaulty) {
                return "Process " + pid + " [faulty]";
            } else {
                return "Process " + pid;

            }
        }
    }

    private void setupSockets() {
        try {
            ServerSocket server = new ServerSocket(controller.getProcessPort(pid));
            switch (pid) {
                case 0: {
                    while (sockets.size() < 6) {
                        acknowledgeConnection(server);
                    }
                    break;
                }
                case 1: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    while (sockets.size() < 6 ) {
                        acknowledgeConnection(server);
                    }
                    break;
                }
                case 2: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    s = new Socket("localhost", controller.getProcessPort(1));
                    identifySelf(s);
                    sockets.put(1, s);
                    while (sockets.size() < 6) {
                        acknowledgeConnection(server);
                    }
                    break;
                }
                case 3: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    s = new Socket("localhost", controller.getProcessPort(1));
                    identifySelf(s);
                    sockets.put(1, s);
                    s = new Socket("localhost", controller.getProcessPort(2));
                    identifySelf(s);
                    sockets.put(2, s);
                    while (sockets.size() < 6) {
                        acknowledgeConnection(server);
                    }
                    break;
                }
                case 4: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    s = new Socket("localhost", controller.getProcessPort(1));
                    identifySelf(s);
                    sockets.put(1, s);
                    s = new Socket("localhost", controller.getProcessPort(2));
                    identifySelf(s);
                    sockets.put(2, s);
                    s = new Socket("localhost", controller.getProcessPort(3));
                    identifySelf(s);
                    sockets.put(3, s);
                    while (sockets.size() < 6) {
                        acknowledgeConnection(server);
                    }
                    break;
                }
                case 5: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    s = new Socket("localhost", controller.getProcessPort(1));
                    identifySelf(s);
                    sockets.put(1, s);
                    s = new Socket("localhost", controller.getProcessPort(2));
                    identifySelf(s);
                    sockets.put(2, s);
                    s = new Socket("localhost", controller.getProcessPort(3));
                    identifySelf(s);
                    sockets.put(3, s);
                    s = new Socket("localhost", controller.getProcessPort(4));
                    identifySelf(s);
                    sockets.put(4, s);
                    acknowledgeConnection(server);
                    break;
                }
                case 6: {
                    Socket s = new Socket("localhost", controller.getProcessPort(0));
                    identifySelf(s);
                    sockets.put(0, s);
                    s = new Socket("localhost", controller.getProcessPort(1));
                    identifySelf(s);
                    sockets.put(1, s);
                    s = new Socket("localhost", controller.getProcessPort(2));
                    identifySelf(s);
                    sockets.put(2, s);
                    s = new Socket("localhost", controller.getProcessPort(3));
                    identifySelf(s);
                    sockets.put(3, s);
                    s = new Socket("localhost", controller.getProcessPort(4));
                    identifySelf(s);
                    sockets.put(4, s);
                    s = new Socket("localhost", controller.getProcessPort(5));
                    identifySelf(s);
                    sockets.put(5, s);
                    break;
                }
                default: {
                    System.out.println("defaulting");
                }
            }
            controller.processfinished(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void acknowledgeConnection(ServerSocket server) {
        try {
            Socket s = server.accept();
            reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
            String processName = reader.readLine();
            sockets.put(Integer.parseInt(processName), s);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void identifySelf(Socket s) {
        try {
            writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
            writer.write(pid + "\n");
            writer.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
