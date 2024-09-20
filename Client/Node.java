package Client;

import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Node {

    private ArrayList<Node> fingerTable;
    
    private String ip;
    private int port;
    private BigInteger hash; // Hash of the ip and port to identify the node order

    public Node(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public ArrayList<Node> getFingerTable() {
        return this.fingerTable;
    }

    public int getPort() {
        return this.port;
    }

    public String getIp() {
        return this.ip;
    }

    public BigInteger getHashNumber() {
        return this.hash;
    }

    private void setFingerTable(ArrayList<Node> fingerTable) {
        this.fingerTable = fingerTable;
    }

    /**
     * Calculates the hash of the node and updates its value
     * 
     * @throws NoSuchAlgorithmException
     */
    private void calculateHash() throws NoSuchAlgorithmException {
        String msg = ip + port;
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(msg.getBytes());

        // Convert the hash bytes directly to a BigInteger
        BigInteger hashNumber = new BigInteger(1, hashBytes);
        System.out.println("Hash as Decimal Number: " + hashNumber);

        this.hash = hashNumber;
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
     * Creates a String representation of the finger table of the node
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
                "fingerTable = " + getFingerTableString() + ", " +
                "ip = " + ip + ", " +
                "port = " + port + ", " +
                "hash = " + hash + ']';
    }

    // ===========================================================================================
    //                                    Imports do be hard
    // ===========================================================================================

    public static boolean isPowerOfTwo(int n) {
        return (n > 0) && ((n & (n - 1)) == 0);
    }

    // ===========================================================================================
    //                                          Tests
    // ===========================================================================================

    public static void main(String[] args) throws UnknownHostException, NoSuchAlgorithmException {

        // Get the local host address
        InetAddress localHost = InetAddress.getLocalHost();
        String ipAddress = localHost.getHostAddress();

        // Create the nodes
        Node node1 = new Node(ipAddress, 8081);
        Node node2 = new Node(ipAddress, 8082);
        Node node3 = new Node(ipAddress, 8083);
        Node node4 = new Node(ipAddress, 8084);
        Node node5 = new Node(ipAddress, 8085);

        // create the hashes of the nodes
        node1.calculateHash();
        node2.calculateHash();
        node3.calculateHash();
        node4.calculateHash();
        node5.calculateHash();
    }
}