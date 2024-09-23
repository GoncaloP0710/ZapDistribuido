package client;

import java.util.ArrayList;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

import dtos.NodeDTO;
import handlers.NodeService;
import client.User;

public class Node {

    // ---------------------- Default Node ----------------------
    private String ipDefault = "";
    private int portDefault = 0;
    // ----------------------------------------------------------

    private User user;

    private ArrayList<NodeDTO> fingerTable;
    private NodeDTO nextNode;
    
    private String ip;
    private int port;

    private int hashLength = 160; // Length of the hash in bits (SHA-1)
    private int ringSize = (int) Math.pow(2, hashLength); // Size of the ring (2^160)
    private BigInteger hash; // Hash of the ip and port to identify the node order

    // ---------------------- key store ----------------------
    String keystoreFile;
    String keystorePassword;
    // -------------------------------------------------------

    // ---------------------- trust store --------------------
    String truststoreFile;
    // String truststorePassword;
    // -------------------------------------------------------

    public Node(String ip, int port, String keystoreFile, String keystorePassword) throws NoSuchAlgorithmException {
        this.fingerTable = new ArrayList<>();
        this.ip = ip;
        this.port = port;
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

    public int getRingSize() {
        return this.ringSize;
    }

    public void setFingerTable(ArrayList<NodeDTO> fingerTable) {
        this.fingerTable = fingerTable;
    }

    public void setNextNode(NodeDTO nextNode) {
        this.nextNode = nextNode;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setHashNumber(BigInteger hash) {
        this.hash = hash;
    }

    public void setKeystoreFile(String keystoreFile) {
        this.keystoreFile = keystoreFile;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public void setTruststoreFile(String truststoreFile) {
        this.truststoreFile = truststoreFile;
    }

    /**
     * Instead of the ip and port we use name to be easier to find the user in the ring
     * 
     * @return
     * @throws NoSuchAlgorithmException
     */
    private BigInteger calculateHash() throws NoSuchAlgorithmException {
        // Instead of the ip and port we use name to be easier to find the user in the ring
        String msg = user.getUserName(); 
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(msg.getBytes());

        // Convert the hash bytes directly to a BigInteger
        BigInteger hashNumber = new BigInteger(1, hashBytes);
        System.out.println("Hash as Decimal Number: " + hashNumber);

        return hashNumber;
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