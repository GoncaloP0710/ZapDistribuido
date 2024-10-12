package Client;

import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import Events.*;
import Interface.UserServiceInterface;
import Message.Message;
import dtos.*;
import Handlers.EventHandler;
import Utils.*;

/**
 * This class is responsible for the node comunication on the network
 */
public class UserService implements UserServiceInterface {

    // ---------------------- Default Node ----------------------
    private String ipDefault = "";
    private int portDefault = 0;
    // ----------------------------------------------------------

    String keystoreFile;
    String keystorePassword;
    String truststoreFile;
    // String truststorePassword;

    private UserDTO currentUser;
    private Node currentNode;
    private NodeDTO currentNodeDTO;

    private SSLServerSocket serverSocket;
    private EventHandler eventHandler;

    private int hashLength = 160; // Length of the hash in bits (SHA-1)
    private int ringSize = (int) Math.pow(2, hashLength); // Size of the ring (2^160)

    public UserService(Node currentNode, String keystoreFile, String keystorePassword, String truststoreFile) throws IOException {
        this.currentNode = currentNode;
        this.keystoreFile = keystoreFile;
        this.keystorePassword = keystorePassword;
        this.truststoreFile = truststoreFile;
        this.eventHandler = new EventHandler(this);
        currentNodeDTO = new NodeDTO(currentNode.getIp(), currentNode.getPort(), currentNode.getHashNumber());
        startServer(currentNode);
    }

    public void setNextNode(NodeDTO nextNode) {
        currentNode.setNextNode(nextNode);
    }

    public String getIpDefault() {
        return ipDefault;
    }

    public int getPortDefault() {
        return portDefault;
    }

    public Node getCurrentNode() {
        return currentNode;
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public int getRingSize() {
        return ringSize;
    }

    public NodeDTO getCurrentNodeDTO() {
        return currentNodeDTO;
    }

    public int getHashLength() {
        return hashLength;
    }

    public void startServer(Node node) throws IOException {

        this.currentNode = node;

        System.setProperty("javax.net.ssl.keyStore", keystoreFile);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        // Get an SSLSocketFactory from the SSLContext
        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        this.serverSocket = (SSLServerSocket) factory.createServerSocket(currentNode.getPort());

        while (true) {
            Socket clientSocket = null; // other node sockets
            try {
                clientSocket = serverSocket.accept();
                NodeThread newServerThread = new NodeThread(clientSocket, null, this);
                newServerThread.setListener(this);
                newServerThread.start();

            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
    }

    /**
     * Send command to the Node with the respective ip and port
     * 
     * @param ip
     * @param port
     * @param command
     */
    public void startClient(String ip, int port, Message msg) {

        System.setProperty("javax.net.ssl.keyStore", keystoreFile);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        System.setProperty("javax.net.ssl.trustStore", truststoreFile);
        System.setProperty("javax.net.ssl.trustStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.trustStoreType", "JCEKS");
        
        try {
            // Create SSL socket
            SocketFactory factory = SSLSocketFactory.getDefault();
            SSLSocket sslClientSocket = (SSLSocket) factory.createSocket(ip, port);

            NodeThread newClientThread = new NodeThread(sslClientSocket, msg, this);
            newClientThread.start();

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Discovers the node that has the given hash or the closest one to it to continue the search. 
     * If the network is empty or the node with a given hash does not exist, null is returned.
     * 
     * @param startNode
     * @param node
     * @return
     */
    public NodeDTO getNodeWithHash(BigInteger node) {
        if (currentNode.getFingerTable().isEmpty()) // If there are no nodes on the finger table
            return null;

        int distanceToNext = Utils.getDistance(currentNode.getHashNumber(), currentNode.getNextNode().getHash(), getRingSize());
        int distanceToNode = Utils.getDistance(currentNode.getHashNumber(), node, getRingSize());
        if (distanceToNext > distanceToNode) // If the node is between the current node and the next node (in these case the node does not exist)
            return null;

        NodeDTO closestFingerNode = null;
        for (NodeDTO fingerNode : currentNode.getFingerTable()) {
            BigInteger fingerNodeHash = fingerNode.getHash();

            if (fingerNodeHash.equals(node)) // If the node is in the finger table
                return fingerNode;

            if (closestFingerNode == null || 
                (fingerNodeHash.intValue() > closestFingerNode.getHash().intValue() && 
                fingerNodeHash.intValue() < node.intValue())) // If these node is the closest to the node
                closestFingerNode = fingerNode;
        }
        return closestFingerNode;
    }

    /**
     * Makes the changes needed to mantain the network correct when a node exits
     */
    public void exitNode() {
        eventHandler.exitNode();
    }

    @Override
    public void processEvent(NodeEvent e) {
        if (e instanceof EnterNodeEvent) {
            eventHandler.enterNode((EnterNodeEvent) e);
        } else if (e instanceof UpdateNeighboringNodesEvent) {
            eventHandler.updateNeighbors((UpdateNeighboringNodesEvent) e);
        } else if (e instanceof UpdateNodeFingerTableEvent) {
            eventHandler.updateFingerTable((UpdateNodeFingerTableEvent) e);
        } else if (e instanceof BroadcastUpdateFingerTableEvent) {
            eventHandler.broadcastMessage(((BroadcastUpdateFingerTableEvent) e));
        } else if (e instanceof NodeSendMessageEvent) {
            // TODO: Send message to the node
        } else {
            throw new UnsupportedOperationException("Unhandled event type");
        }
    }

}
