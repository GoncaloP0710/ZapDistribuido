package client;

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
import handlers.EventHandler;
import utils.*;

/**
 * This class is responsible for the node comunication on the network
 */
public class UserService implements UserServiceInterface{

    // ---------------------- Default Node ----------------------
    private String ipDefault = "";
    private int portDefault = 0;
    // ----------------------------------------------------------

    // ---------------------- key store ----------------------
    String keystoreFile;
    String keystorePassword;
    // -------------------------------------------------------

    // ---------------------- trust store --------------------
    String truststoreFile;
    // String truststorePassword;
    // -------------------------------------------------------

    private UserDTO currentUser;
    private Node currentNode;
    private SSLServerSocket serverSocket;
    private EventHandler eventHandler;

    public UserService(Node currentNode, String keystoreFile, String keystorePassword, String truststoreFile) throws IOException {
        this.currentNode = currentNode;
        this.keystoreFile = keystoreFile;
        this.keystorePassword = keystorePassword;
        this.truststoreFile = truststoreFile;
        this.eventHandler = new EventHandler(this);
        startServer(currentNode);
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
     * 
     * 
     * @param nextNode
     */
    public void setNextNode(NodeDTO nextNode) {
        currentNode.setNextNode(nextNode);
    }

    /**
     * 
     * 
     * @return
     */
    public String getIpDefault() {
        return ipDefault;
    }

    /**
     * 
     * 
     * @return
     */
    public int getPortDefault() {
        return portDefault;
    }

    public Node getCurrentNode() {
        return currentNode;
    }

    public NodeDTO getCurrentNodeDTO() {
        return currentNode.getNodeDTO();
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    /**
     * Discovers the node that has the given hash or the closest one to it to continue the search
     * 
     * @param startNode
     * @param node
     * @return
     */
    public NodeDTO getNodeWithHash(BigInteger node) {
        if (currentNode.getFingerTable().isEmpty()) // If there are no nodes on the finger table
            return null;

        int distanceToNext = Utils.getDistance(currentNode.getHashNumber(), currentNode.getNextNode().getHash(), currentNode.getRingSize());
        int distanceToNode = Utils.getDistance(currentNode.getHashNumber(), node, currentNode.getRingSize());
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

    @Override
    public void processEvent(NodeEvent e) { // TODO: Add more events if necessary
        if (e instanceof EnterNodeEvent) {
            eventHandler.enterNode((EnterNodeEvent) e);
        } else if (e instanceof UpdateNodeFingerTableEvent) {
            // TODO: Implement the update finger table event handling func
        } else {
            throw new UnsupportedOperationException("Unhandled event type");
        }
    }

}
