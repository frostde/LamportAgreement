/*Daniel Frost
* Eliza Karki
* Project 3 Lamport Agreement Algorithm*/

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
    public int value;
    public int initialValue;
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
                value = new Random().nextInt(2);
                Message m = new Message(value);
                m.path.add(pid);
                m.outValue = value;
                for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                    writer = new BufferedWriter(new OutputStreamWriter(entry.getValue().getOutputStream()));
                    if (isFaulty) {
                        Message faultyMessage = new Message(new Random().nextInt(2));
                        faultyMessage.path.add(pid);
                        writer.write(faultyMessage + "\n");
                        System.out.println(pid + " wrote " + faultyMessage.toString() + " to " + entry.getKey());

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
                        Message m;
                        if (isFaulty) {
                            m = new Message(-1);
                        } else {
                            m = new Message(Integer.parseInt(x.substring(x.indexOf('{') + 1, x.indexOf(','))));
                        }
                        m.path.add(initiatorpid);
                        m.outValue = m.inValue;
                        Vector<Message> v = new Vector<>();
                        v.add(m);
                        tree.insert(v, 0);
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
            Vector<Message> t = new Vector<>();
            Message nw = new Message();
            for (Message m : newMessages) {
                nw = new Message(m);
                nw.path.add(pid);
                nw.outValue = nw.inValue;
                t.add(nw);
                sb.append(nw.toString() + "\n");
            }
            if (!isInitiator) {
                for (int i = 0; i < 5; i++) {
                    System.out.println("Process " + pid + " sent " + nw.toString() + " to process " + i);
                }
            }
            tree.insert(t, round);
            for (Map.Entry<Integer, Socket> entry : sockets.entrySet()) {
                if (entry.getKey() != initiatorpid) {
                    writer = new BufferedWriter(new OutputStreamWriter(entry.getValue().getOutputStream()));
                    writer.write(sb.toString());
                    writer.flush();
                }
            }
            System.out.println("\n");
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
                            n.outValue = n.inValue;
                            newMessages.add(n);
                        } else {
                            int c;
                            StringBuilder sb = new StringBuilder();
                            while (is.available() != 0) {
                                c = is.read();
                                sb.append((char)c);
                            }
                            for (String s : sb.toString().split("\n") ) {
                                Message n = new Message(Integer.parseInt(s.substring(s.indexOf('{')+1, s.indexOf(','))));
                                String path = s.substring(s.indexOf('[')+1, s.indexOf(']'));
                                path = path.replaceAll("\\D+","");
                                for (char x : path.toCharArray()) {
                                    n.path.add(Integer.parseInt(Character.toString(x)));
                                }
                                n.outValue = n.inValue;
                                newMessages.add(n);
                            }
                        }
                    }
                }
                tree.insert(newMessages, round);
            }
            controller.processfinished(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public String identify() {
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

    public int TraverseTree(MessageTree tree) {

        int childCount = tree.root.children.size();
            if (childCount == 0) {

            } else {
                    if (tree.root.children.get(0).children.size() == 0) {
                        return majorityOfChildren(tree.root);
                    }
                for (int i = 0; i < childCount - 1; i++) {
                    MessageTree child = tree.getChildTree(i);
                    if (child.root.children.size() > 0) {
                        if (child.root.children.get(0).children.size() == 0) {
                            return majorityOfChildren(child.root);
                        }
                    }
                    return TraverseTree(child);
                }
            }

        return 99;
    }

    public int majorityOfChildren(MessageNode node) {
        if (node.children.size() == 0) {
            return -99;
        }
        return this.tree.majority(node.children);
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
