package Client;

import java.io.IOException;
import java.io.File;
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import Events.*;
import Interface.UserServiceInterface;
import Message.*;
import dtos.*;
import Handlers.EncryptionHandler;
import Handlers.EventHandler;
import Handlers.InterfaceHandler;
import Handlers.KeyHandler;
import Utils.*;

/**
 * This class is responsible for the node comunication on the network
 */
public class UserService implements UserServiceInterface {

    // ---------------------- Default Node ----------------------
    private String ipDefault = "localhost";
    private int portDefault = 8080;
    private String usernameDefault = "a";
    // ----------------------------------------------------------

    KeyHandler keyHandler;
    File keystoreFile;
    String keystorePassword;
    File truststoreFile;

    private Node currentNode;
    private NodeDTO currentNodeDTO;

    private final Lock nodeSendMessageLock = new ReentrantLock();

    private ServerSocket serverSocket;
    private EventHandler eventHandler;
    public String username;

    private int hashLength = 160; // Length of the hash in bits (SHA-1)
    private int ringSize = (int) Math.pow(2, hashLength); // Size of the ring (2^160)

    public UserService(String username, Node currentNode, KeyHandler keyHandler) throws Exception {
        this.username = username;
        this.currentNode = currentNode;
        this.keyHandler = keyHandler;
        this.keystoreFile = keyHandler.getKeystoreFile();
        this.keystorePassword = keyHandler.getKeyStorePassword();
        this.truststoreFile = keyHandler.getTrustStoreFile();
        initializeCurrentNodeDTO(username, currentNode, keyHandler.getCertificate(username));
        this.eventHandler = new EventHandler(this);

        // ------------------ Start servers ------------------
        startServerInThread(currentNode, false);
        startServerInThread(currentNode, true);
        // ---------------------------------------------------

        if (ipDefault.equals(currentNode.getIp()) && portDefault == currentNode.getPort()) { // Check for default node
            currentNode.setNextNode(currentNodeDTO);
            currentNode.setPreviousNode(currentNodeDTO);
        } else { // If it is not the default node, it has to enter the network
            ChordInternalMessage message = new ChordInternalMessage(MessageType.EnterNode, currentNodeDTO);
            startClient(ipDefault, portDefault, message, false, usernameDefault);
        }        
    }

    public void initializeCurrentNodeDTO(String username, Node currentNode, Certificate cer) {
        this.currentNodeDTO = new NodeDTO(username, currentNode.getIp(), currentNode.getPort(), currentNode.getHashNumber(), cer);
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

    public int getRingSize() {
        return ringSize;
    }

    public synchronized NodeDTO getCurrentNodeDTO() {
        return this.currentNodeDTO;
    }

    public int getHashLength() {
        return hashLength;
    }

    public KeyHandler getKeyHandler() {
        return keyHandler;
    }

    public String getUsername() {
        return username;
    }

    // ============================== SERVER ==============================

    public void startServerInThread(Node node, boolean secure) {
        Runnable serverTask = () -> {
            try {
                if (secure) {
                    startServer(node);
                } else {
                    serverInsecure(node);
                }
            } catch (IOException | KeyStoreException | InterruptedException e) {
                System.err.println("Error starting server: " + e.getMessage());
                e.printStackTrace();
            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();
    }

    public void startServer(Node node) throws IOException, KeyStoreException {

        System.setProperty("javax.net.ssl.keyStore", keystoreFile.toString());
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", "JKS");

        System.setProperty("javax.net.ssl.trustStore", truststoreFile.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

        String ip = node.getIp();
        int port = node.getPort();

        // Get an SSLSocketFactory from the SSLContext
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        this.serverSocket = (SSLServerSocket) factory.createServerSocket(port);

        while (true) {
            Socket clientSocket = null; // other node sockets
            try {
                if(serverSocket != null){
                    System.out.println(serverSocket.toString());
                }
                clientSocket = serverSocket.accept();
                NodeThread newServerThread = new NodeThread(clientSocket, null, this, keyHandler);
                newServerThread.start();
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
    }

    public void serverInsecure(Node node) throws IOException, InterruptedException {
        this.currentNode = node;
        String ip = node.getIp();
        int port = node.getPort()+1;

        System.out.println("Starting server on " + ip + ":" + port);
        // Use normal ServerSocket instead of SSLServerSocket
        ServerSocket sSocket = new ServerSocket(port); //socket novo

        while (true) {
            Socket clientSocket = null; // other node sockets
            try {
                System.err.println("Server socket waiting for connection..." + sSocket.getLocalPort()); //PRINT MVP
                clientSocket = sSocket.accept();
                NodeThread newServerThread = new NodeThread(clientSocket, null, this, keyHandler);
                newServerThread.start();
                newServerThread.join(); // Wait for the thread to finish
                System.out.println(keyHandler.getTruststorePath());
                Utils.loadTrustStore(keyHandler.getTruststorePath(), keyHandler.getKeyStorePassword());
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
    }

    // ============================== CLIENT ==============================

    /**
     * Send command to the Node with the respective ip and port
     * 
     * @param ip
     * @param port
     * @param command
     */
    public void startClient(String ip, int port, Message msg, boolean waitForResponse, String alias) {
        try {
            if (!keyHandler.getTruStore().containsAlias(alias)) // If the certificate of the other node is not in the truststore
                shareCertificateClient(ip, port, new ChordInternalMessage(MessageType.addCertificateToTrustStore, keyHandler.getCertificate(alias), alias, currentNodeDTO.getUsername()));

            System.setProperty("javax.net.ssl.keyStore", keystoreFile.toString());
            System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
            System.setProperty("javax.net.ssl.keyStoreType", "JKS");

            System.setProperty("javax.net.ssl.trustStore", truststoreFile.toString());
            System.setProperty("javax.net.ssl.trustStorePassword", keystorePassword);
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");

            // Create SSL socket
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslClientSocket = (SSLSocket) factory.createSocket(ip, port);

            NodeThread newClientThread = new NodeThread(sslClientSocket, msg, this, keyHandler);
            newClientThread.start();
    
            if (waitForResponse) 
                newClientThread.join(); // Wait for the thread to finish
    
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Creates a new conection to the node with the given ip and port to share the certificate
     * 
     * @param ip
     * @param port
     * @param msg
     * @throws NoSuchAlgorithmException
     */
    private void shareCertificateClient(String ip, int port, Message msg) throws NoSuchAlgorithmException {
        try {
            System.out.println("Start of shareCertificateClient");
            // Create normal socket
            System.out.println("Creating socket...");
            System.out.println("IP: " + ip + " Port: " + (port+1));
            Socket clientSocket = new Socket(ip, (port+1));
            NodeThread newClientThread = new NodeThread(clientSocket, msg, this, keyHandler);
            newClientThread.start();
            newClientThread.join(); // Wait for the thread to finish
            System.out.println(keyHandler.getTruststorePath());
            Utils.loadTrustStore(keyHandler.getTruststorePath(), keyHandler.getKeyStorePassword());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    // ==================================================================

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

    public void sendMessage(InterfaceHandler interfaceHandler) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, Exception {
        nodeSendMessageLock.lock();
        try {
            System.out.println("Select the user you want to send a message to: ");
            String reciver = interfaceHandler.getInput();
            System.out.println("Write the message: ");
            byte[] message = interfaceHandler.getInput().getBytes();
            
            NodeDTO reciverNode = currentNode.belongsToFingerTable(reciver);
            BigInteger reciverHash = currentNode.calculateHash(reciver);
            
            if (reciverNode != null) { // Reciver is on the finger table
                System.out.println("Reciver is on the finger table");
                byte[] messageEncryp = EncryptionHandler.encryptWithPubK(message, reciverNode.getPubK());
                UserMessage userMessage = new UserMessage(MessageType.SendMsg, currentNodeDTO, reciverHash, messageEncryp);
                startClient(reciverNode.getIp(), reciverNode.getPort(), userMessage, false, reciverNode.getUsername());
            } else { // Reciver is not on the finger table so we have to find its pubK
                System.out.println("Reciver is not on the finger table");
                eventHandler.addMessage(reciverHash, message);
                RecivePubKeyEvent event = new RecivePubKeyEvent(new ChordInternalMessage(MessageType.RecivePubKey, null, reciverHash, currentNodeDTO));
                eventHandler.recivePubKey(event);
            }
        } finally {
            nodeSendMessageLock.unlock();
        }
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
            eventHandler.sendUserMessage(((NodeSendMessageEvent) e));
        } else if (e instanceof AddCertificateToTrustStoreEvent) {
            eventHandler.addCertificateToTrustStore((AddCertificateToTrustStoreEvent) e);
        } else if (e instanceof RecivePubKeyEvent) {
            eventHandler.recivePubKey((RecivePubKeyEvent) e);
        } else {
            System.out.println("Exception class: " + e.getClass().getName());
            System.out.println("Exception instance: " + e.toString());
            throw new UnsupportedOperationException("Unhandled event type");
        }
    }
}