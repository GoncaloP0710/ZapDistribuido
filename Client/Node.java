package Client;

import java.util.ArrayList;

public class Node {

    private Node nextNode;
    private Node prevNode;
    private ArrayList<Node> fingerTable;
    
    private String ip;
    private int port;
    private byte[] hash; // Hash of the ip and port to identify the node order

    public Node(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    private int getPort() {
        return this.port;
    }

    private String getIp() {
        return this.ip;
    }
    
    public Node nextNode() {
        return this.nextNode;
    }
    
    public Node prevNode() {
        return this.prevNode;
    }

    private ArrayList<Node> getUpdateFingerTable(int counter, Node node, ArrayList<Node> fingerTable) {
        if (this.equals(node))
            return fingerTable;
        if (Utils.isPowerOfTwo(counter)) {
            fingerTable.add(node);
            getUpdateFingerTable(counter++, node.nextNode(), fingerTable);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == this) 
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        var that = (Node) obj;
        return this.ip == that.ip && this.port == that.port;
    }
}