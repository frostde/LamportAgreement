/*Daniel Frost
* Eliza Karki
* Project 3 Lamport Agreement Algorithm*/

import java.util.Vector;

/**
 * Created by danielfrost on 5/2/18.
 */
public class MessageTree {
    public MessageNode root;
    int ownerpid;

    public MessageTree(int p) {
        this.ownerpid = p;
    }

    public MessageTree() {

    }

    private MessageNode findParent(Message m, int r) {
        int i = 1;
        MessageNode node = root;

        while (i < r) {
            for (MessageNode n : node.children) {
                if (n.message.prefixMatch(m.path)) {
                    node = n;
                    break;
                }
            }
            i++;
        }
        return node;
    }

    public void insert(Vector<Message> messages, int round) {
        if (round == 0) {
            root = new MessageNode();
            root.parent = null;
            root.message = messages.firstElement();
            //System.out.println("\t[" + ownerpid + "] insert(..round 0..) -> root " + root);
        } else {
            for (Message m : messages) {
                MessageNode parent = findParent(m, round);
                MessageNode node = new MessageNode();
                node.message = m;
                parent.children.add(node);
                //System.out.println("\t[" + ownerpid + "]insert(" + m + "," + round + " ) -> parent " + parent.message);
            }
        }
    }

    public void insert(Message m, int round) {
        if (round == 0) {
            root = new MessageNode();
            root.parent = null;
            root.message = m;
            System.out.println("\t[" + ownerpid + "] insert(..round 0..) -> root " + root);
        } else {
            MessageNode parent = findParent(m, round);
            MessageNode node = new MessageNode();
            node.message = m;
            parent.children.add(node);
            System.out.println("\t[" + ownerpid + "]insert(" + m + "," + round + " ) -> parent " + parent.message);
        }
    }


    /*public int decideValue() {
        decide(0);
    }*/

    public MessageTree getChildTree(int i) {
        MessageNode node = this.root.children.get(i);
        MessageTree tree = new MessageTree();
        tree.root = node;
        return tree;
    }

    public int majority(Vector<MessageNode> messages) {
        int ones = 0;
        int zeros = 0;
        int faulties = 0;
        for (MessageNode m : messages) {
            switch (m.message.outValue) {
                case 0: {
                    zeros++;
                    break;
                }
                case 1: {
                    ones++;
                    break;
                }
                case -1: {
                    faulties++;
                    break;
                }
            }
        }
        if (ones >= zeros && ones >= faulties) {
            if (ones == zeros) {
                return 0;
            }

            if (ones == faulties) {
                return 1;
            }
            return 1;
        } else if (zeros >= ones && zeros >= faulties) {
            if (ones == zeros || zeros == faulties) {
                return 0;
            }
            return 0;
        }
        return -1;
    }


}
