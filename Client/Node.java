package client;

import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import dtos.NodeDTO;
import handlers.NodeService;

public class Node {

    private ArrayList<NodeDTO> fingerTable;
    private NodeDTO nextNode;
    
    private String ip;
    private int port;

    // ---------------------- key store ----------------------
    String keystoreFile;
    String keystorePassword;
    // -------------------------------------------------------

    // ---------------------- trust store ----------------------
    String truststoreFile;
    // String truststorePassword;
    // -------------------------------------------------------

    private NodeService nodeService;

    // TODO: Change to 160 or the respective value after testing
    private int hashLength = 4; // Length of the hash in bits (SHA-1)
    private int ringSize = (int) Math.pow(2, hashLength); // Size of the ring (2^160)
    private BigInteger hash; // Hash of the ip and port to identify the node order

    public Node(String ip, int port, String keystoreFile, String keystorePassword) throws NoSuchAlgorithmException {
        this.fingerTable = new ArrayList<>();
        this.ip = ip;
        this.port = port;
        this.nodeService = new NodeService(this);
        setHashNumber(calculateHash());
    }

    public ArrayList<NodeDTO> getFingerTable() {
        return this.fingerTable;
    }

    public NodeDTO getNextNode() {
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

    public String getKeystoreFile() {
        return this.keystoreFile;
    }

    public String getKeystorePassword() {
        return this.keystorePassword;
    }

    public String getTruststoreFile() {
        return this.truststoreFile;
    }

    private void setFingerTable(ArrayList<NodeDTO> fingerTable) {
        this.fingerTable = fingerTable;
    }

    private void setNextNode(NodeDTO nextNode) {
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

    private void setKeystoreFile(String keystoreFile) {
        this.keystoreFile = keystoreFile;
    }

    private void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    private void setTruststoreFile(String truststoreFile) {
        this.truststoreFile = truststoreFile;
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
    private ArrayList<NodeDTO> getUpdateFingerTable(int counter, NodeDTO node, ArrayList<NodeDTO> fingerTable) {
        if (this.equals(node) || counter == this.hashLength)
            return fingerTable;
        int distance = (node.getHash().intValue() - this.hash.intValue()+ ringSize) % ringSize;

        if (distance >= Math.pow(2, counter)) {
            fingerTable.add(node);

            // Skip the next counters if 2^counter is lower than the distance between this and the next node to try
            while (Math.pow(2, counter) <= distance) 
                counter++;
        }
        // TODO: Implement a way to get the next node DTO
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
        for (NodeDTO node : fingerTable) {
            sb.append("IP: ").append(node.getIp())
                .append(", Port: ").append(node.getPort())
                .append(", Hash: ").append(node.getHash())
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
}