/*Daniel Frost
* Eliza Karki
* Project 3 Lamport Agreement Algorithm*/

import java.util.Vector;

/**
 * Created by danielfrost on 5/2/18.
 */
public class Message {
    public int inValue;
    public int outValue = -1;
    public Vector<Integer> path;

    public Message(int v) {
        this.inValue = v;
        this.path = new Vector<>();
    }

    public Message(Message m2) {
        inValue = m2.inValue;
        path = (Vector<Integer>) m2.path.clone();
    }

    public Message() {

    }

    public int senderPID() {
        return path.lastElement();
    }

    public boolean prefixMatch(Vector<Integer> p) {
        String s = path + ".prefixMatch(" + p + ")";
        for (int i : path) {
            if (i != p.elementAt(path.indexOf(i))) {
                //System.out.println("\t" + s + "-> false");
                return false;
            }
        }
        //System.out.println("\t" + s + "-> true");
        return true;
    }

    public void setInValue(int v) {
        this.inValue = v;
    }


    @Override
    public String toString() {
        return "{" + this.inValue + "," + this.path + ","+ this.outValue + "}";
    }
}
