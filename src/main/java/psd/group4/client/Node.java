package psd.group4.client;

import java.util.ArrayList;

import psd.group4.dtos.NodeDTO;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigInteger;

public class Node {

    private ArrayList<NodeDTO> fingerTable;
    private NodeDTO nextNode;
    private NodeDTO previousNode;
    
    private String ip;
    private int port;
    private BigInteger hash; // Hash of the ip and port to identify the node order

    public Node(String name, String ip, int port) throws NoSuchAlgorithmException {
        this.fingerTable = new ArrayList<>();
        this.ip = ip;
        this.port = port;
        setHashNumber(calculateHash(name));
    }

    public ArrayList<NodeDTO> getFingerTable() {
        return this.fingerTable;
    }

    public NodeDTO getNextNode() {
        return this.nextNode;
    }

    public NodeDTO getPreviousNode() {
        return this.previousNode;
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

    public void setFingerTable(ArrayList<NodeDTO> fingerTable) {
        this.fingerTable = fingerTable;
    }

    public void setNextNode(NodeDTO nextNode) {
        this.nextNode = nextNode;
    }

    public void setPreviousNode(NodeDTO previousNode) {
        this.previousNode = previousNode;
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


    public String neighborsStatus() {
        return previousNode.getUsername() + " - " + " current node " + " - " + nextNode.getUsername();
    }

    /**
     * Check if the node is the default node
     * 
     * @param ip default ip
     * @param port default port
     */
    public boolean checkDefaultNode(String ip, int port) {
        if (this.ip.equals(ip) && this.port == port) {
            setNextNode(null);
            setPreviousNode(null);
            return true;
        }
        return false;
    }

    /**
     * Instead of the ip and port we use name to be easier to find the user in the ring
     * 
     * @param name
     * @return
     * @throws NoSuchAlgorithmException
     */
    public BigInteger calculateHash(String name) throws NoSuchAlgorithmException {
        // Instead of the ip and port we use name to be easier to find the user in the ring
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] hashBytes = md.digest(name.getBytes());

        // Convert the hash bytes directly to a BigInteger
        BigInteger hashNumber = new BigInteger(1, hashBytes);
        return hashNumber;
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
            sb.append("Name: ").append(node.getUsername())
                .append(", IP: ").append(node.getIp())
                .append(", Port: ").append(node.getPort())
                .append(", Hash: ").append(node.getHash())
                .append(", ");
        }
        if (!fingerTable.isEmpty())
            sb.setLength(sb.length() - 2); // Remove the last comma and space
        sb.append("]");
        return sb.toString();
    }

    /**
     * Checks if the node with the name given belongs to the finger table
     * 
     * @param name
     * @return
     */
    public NodeDTO belongsToFingerTable(String name) {
        for (NodeDTO node : fingerTable) {
            if (node.getUsername().equals(name)) {
                return node;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) 
            return true;
        if (obj == null || obj.getClass() != this.getClass())
            return false;
        Node that = (Node) obj;
        return this.ip.equals(that.ip) && this.port == that.port;
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