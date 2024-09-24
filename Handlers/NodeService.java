package handlers;

import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.math.BigInteger;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import Events.NodeEvent;
import Interface.NodeServiceInterface;
import Interface.NodeServiceInterface;
import client.Node;
import dtos.NodeDTO;
import utils.*;
import utils.observer.Listener;

/**
 * This class is responsible for the node comunication on the network
 */
// TODO: Change to normal sockets for testing
public class NodeService implements NodeServiceInterface{

    private Node currentNode;
    private static SSLServerSocket serverSocket;

    private BlockingQueue<String> answerQueue; // Queue to store the answers from the other nodes

    public NodeService(Node currentNode) {
        this.currentNode = currentNode;
        this.answerQueue = new LinkedBlockingQueue<>();
    }

    public void addAnswer(String answer) {
        answerQueue.add(answer);
    }

    public void startServer() throws IOException {

        System.setProperty("javax.net.ssl.keyStore", currentNode.getKeystoreFile());
        System.setProperty("javax.net.ssl.keyStorePassword", currentNode.getKeystorePassword());
        System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        // Get an SSLSocketFactory from the SSLContext
        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        serverSocket = (SSLServerSocket) factory.createServerSocket(currentNode.getPort());

        while (true) {
            Socket clientSocket = null; // other node sockets
            try {
                clientSocket = serverSocket.accept();
                NodeThread newServerThread = new NodeThread(currentNode, clientSocket, null);
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
    public void startClient(String ip, int port, String command) {

        System.setProperty("javax.net.ssl.keyStore", currentNode.getKeystoreFile());
        System.setProperty("javax.net.ssl.keyStorePassword", currentNode.getKeystorePassword());
        System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        System.setProperty("javax.net.ssl.trustStore", currentNode.getTruststoreFile());
        System.setProperty("javax.net.ssl.trustStorePassword", currentNode.getKeystorePassword());
        System.setProperty("javax.net.ssl.trustStoreType", "JCEKS");
        
        try {
            // Create SSL socket
            SocketFactory factory = SSLSocketFactory.getDefault();
            SSLSocket sslClientSocket = (SSLSocket) factory.createSocket(ip, port);

            NodeThread newClientThread = new NodeThread(currentNode, sslClientSocket, command);
            newClientThread.start();

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    public void enterNode(String ipDefault, int portDefault, BigInteger hash) {
        String command = "Enter Node:" + hash;
        startClient(ipDefault, portDefault, command);
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
    public void processEvent(NodeEvent e) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processEvent'");
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'propertyChange'");
    }

}
