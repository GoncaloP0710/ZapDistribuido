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
    
    private Node nextNode() {
        return this.nextNode;
    }
    
    private Node prevNode() {
        return this.prevNode;
    }

    private void setNextNode(Node node) {
        this.nextNode = node;
    }

    private void setPrevNode(Node node) {
        this.prevNode = node;
    }

    private void setFingerTable(ArrayList<Node> fingerTable) {
        this.fingerTable = fingerTable;
    }

    /**
     * Updates all of the finger tables of the nodes in the network
     * 
     * @param node
     */
    private void updateAllFingerTables(Node node) {
        if (this.equals(node))
            return;
        node.setFingerTable(node.getUpdateFingerTable(0, node, new ArrayList<Node>()));
        updateAllFingerTables(node.nextNode());
    }

    /**
     * Obtains the updated finger table of the node
     * 
     * @param counter 
     * @param node
     * @param fingerTable
     * @return
     */
    private ArrayList<Node> getUpdateFingerTable(int counter, Node node, ArrayList<Node> fingerTable) {
        if (this.equals(node))
            return fingerTable;
        if (Utils.isPowerOfTwo(counter))
            fingerTable.add(node);
        getUpdateFingerTable(++counter, node.nextNode(), fingerTable);
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

    /**
     * Creates a representation of the finger table of the node
     * 
     * @return
     */
    private String getFingerTableString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FingerTable: [");
        for (Node node : fingerTable) {
            sb.append(node.toString()).append(", ");
        }
        if (!fingerTable.isEmpty())
            sb.setLength(sb.length() - 2); // Remove the last comma and space
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Node[" +
                "nextNode = " + nextNode.ip + ":" + nextNode.port + ", " +
                "prevNode = " + prevNode.ip + ":" + prevNode.port + ", " +
                "fingerTable = " + getFingerTableString() + ", " +
                "ip = " + ip + ", " +
                "port = " + port + ", " +
                "hash = " + hash + ']';
    }
}