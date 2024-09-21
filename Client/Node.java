package Client;

import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Node {

    private ArrayList<Node> fingerTable;
    private Node nextNode;
    
    private String ip;
    private int port;

    // TODO: Change to 160 or the respective value after testing
    private int hashLength = 4; // Length of the hash in bits (SHA-1)
    private int ringSize = (int) Math.pow(2, hashLength); // Size of the ring (2^160)
    private BigInteger hash; // Hash of the ip and port to identify the node order

    public Node(String ip, int port) throws NoSuchAlgorithmException {
        this.fingerTable = new ArrayList<>();
        this.ip = ip;
        this.port = port;
        setHashNumber(calculateHash());
    }

    public ArrayList<Node> getFingerTable() {
        return this.fingerTable;
    }

    public Node getNextNode() {
        return this.nextNode;
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

    private void setNextNode(Node nextNode) {
        this.nextNode = nextNode;
    }

    private void setPort(int port) {
        this.port = port;
    }

    private void setIp(String ip) {
        this.ip = ip;
    }

    private void setHashNumber(BigInteger hash) {
        this.hash = hash;
    }

    /**
     * 
     * 
     * @return
     * @throws NoSuchAlgorithmException
     */
    private BigInteger calculateHash() throws NoSuchAlgorithmException {
        String msg = ip + port;
        MessageDigest md = MessageDigest.getInstance("SHA-1"); //TODO: O Joao disse para usar o 256, devemos mudar?
        byte[] hashBytes = md.digest(msg.getBytes());

        // Convert the hash bytes directly to a BigInteger
        BigInteger hashNumber = new BigInteger(1, hashBytes);
        System.out.println("Hash as Decimal Number: " + hashNumber);

        return hashNumber;
    }

    /**
     * Obtains the updated finger table of the node
     * 
     * @param counter has to be initializad as 0!
     * @param node
     * @param fingerTable
     * @return
     */
    private ArrayList<Node> getUpdateFingerTable(int counter, Node node, ArrayList<Node> fingerTable) {
        if (this.equals(node) || counter == this.hashLength)
            return fingerTable;
        int distance = (node.hash.intValue() - this.hash.intValue()+ ringSize) % ringSize;

        if (distance >= Math.pow(2, counter)) {
            fingerTable.add(node);

            // Skip the next counters if 2^counter is lower than the distance between this and the next node to try
            while (Math.pow(2, counter) <= distance) 
                counter++;
        }
        return getUpdateFingerTable(counter, node.nextNode, fingerTable);
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
            sb.append("IP: ").append(node.getIp())
                .append(", Port: ").append(node.getPort())
                .append(", Hash: ").append(node.getHashNumber())
                .append(", ");
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
        Node node6 = new Node(ipAddress, 8086);
        Node node7 = new Node(ipAddress, 8087);

        // Set Custom hash values for testing purposes
        node1.setHashNumber(new BigInteger("0"));
        node2.setHashNumber(new BigInteger("1"));
        node3.setHashNumber(new BigInteger("3"));
        node4.setHashNumber(new BigInteger("7"));
        node5.setHashNumber(new BigInteger("9"));
        node6.setHashNumber(new BigInteger("11"));
        node7.setHashNumber(new BigInteger("14"));

        // Create the finger tables first position (successor)
        // node1 -> node2 -> node3 -> node4 -> node5 -> node6 -> node7 -> node1
        // A ser implementado! 
        node1.setNextNode(node2);
        node2.setNextNode(node3);
        node3.setNextNode(node4);
        node4.setNextNode(node5);
        node5.setNextNode(node6);
        node6.setNextNode(node7);
        node7.setNextNode(node1);

        // Update the finger tables
        node1.setFingerTable( node1.getUpdateFingerTable(0, node1.getNextNode(), new ArrayList<>()) );
        node2.setFingerTable( node2.getUpdateFingerTable(0, node2.getNextNode(), new ArrayList<>()) );
        //node3.setFingerTable( node3.getUpdateFingerTable(0, node3.getNextNode(), new ArrayList<>()) );
        node4.setFingerTable( node4.getUpdateFingerTable(0, node4.getNextNode(), new ArrayList<>()) );
        node5.setFingerTable( node5.getUpdateFingerTable(0, node5.getNextNode(), new ArrayList<>()) );
        //node6.setFingerTable( node6.getUpdateFingerTable(0, node6.getNextNode(), new ArrayList<>()) );
        //node7.setFingerTable( node7.getUpdateFingerTable(0, node7.getNextNode(), new ArrayList<>()) );

        // Print the finger tables
        System.out.println("Node 1: " + node1.toString());
        System.out.println("Node 2: " + node2.toString());
        //System.out.println("Node 3: " + node3.toString());
        System.out.println("Node 4: " + node4.toString());
        System.out.println("Node 5: " + node5.toString());
        //System.out.println("Node 6: " + node6.toString());
        //System.out.println("Node 7: " + node7.toString());
    }
}