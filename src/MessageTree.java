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
            System.out.println("\t[" + ownerpid + "] insert(..round 0..) -> root " + root);
        } else {
            for (Message m : messages) {
                MessageNode parent = findParent(m, round);
                MessageNode node = new MessageNode();
                node.message = m;
                parent.children.add(node);
                System.out.println("\t[" + ownerpid + "]insert(" + m + "," + round + " ) -> parent " + parent.message);
            }
        }
    }

    public void insert(Message m, int round) {
        if (round == 0) {
            root = new MessageNode();
            root.parent = null;
            root.message = m;
            //System.out.println("\t[" + ownerpid + "] insert(..round 0..) -> root " + root);
        } else {
            MessageNode parent = findParent(m, round);
            MessageNode node = new MessageNode();
            node.message = m;
            parent.children.add(node);
            //System.out.println("\t[" + ownerpid + "]insert(" + m + "," + round + " ) -> parent " + parent.message);
        }
    }





}
