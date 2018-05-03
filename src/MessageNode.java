/*Daniel Frost
* Eliza Karki
* Project 3 Lamport Agreement Algorithm*/

import java.util.Vector;

/**
 * Created by danielfrost on 5/2/18.
 */
public class MessageNode {
    public Message message;
    public MessageNode parent;
    public Vector<MessageNode> children;

    public MessageNode() {
        children = new Vector<>();
    }
}
