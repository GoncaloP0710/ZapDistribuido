package Client;

import java.io.File;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import Events.*;
import Interface.UserServiceInterface;
import Message.*;
import dtos.*;
import Handlers.*;
import Utils.*;

/**
 * This class is responsible for the node comunication on the network
 */
public class UserService implements UserServiceInterface {

   // ---------------------- Default Node ----------------------
    private String ipDefault = "localhost";
    private int portDefault = 8080;
    private String usernameDefault = "Wang";
    // ----------------------------------------------------------

    // ---------------------- Chord Variables --------------------
    private int hashLength = 160; // Length of the hash in bits (SHA-1)
    private int ringSize = (int) Math.pow(2, hashLength); // Size of the ring (2^160)
    // -----------------------------------------------------------
    
    // ---------------------- Security Variables ----------------
    KeyHandler keyHandler;
    File keystoreFile;
    String keystorePassword;
    File truststoreFile;
    // -----------------------------------------------------------

    // ---------------------- User Variables ---------------------
    public String username;
    private Node currentNode;
    private NodeDTO currentNodeDTO;
    private final Lock nodeSendMessageLock = new ReentrantLock();
    private EventHandler eventHandler;
    private NodeClientHandler clientHandler;
    private NodeServerHandler serverHandler;
    // -----------------------------------------------------------

    public UserService(String username, Node currentNode, KeyHandler keyHandler) throws Exception {
        this.username = username;
        this.currentNode = currentNode;
        this.keyHandler = keyHandler;
        this.keystoreFile = keyHandler.getKeystoreFile();
        this.keystorePassword = keyHandler.getKeyStorePassword();
        this.truststoreFile = keyHandler.getTrustStoreFile();
        initializeCurrentNodeDTO(username, currentNode, keyHandler.getCertificate(username));
        this.clientHandler = new NodeClientHandler(this);
        this.serverHandler = new NodeServerHandler(this);
        this.eventHandler = new EventHandler(this);

        // ------------------ Start servers ------------------
        serverHandler.startServerInThread(currentNode, false);
        serverHandler.startServerInThread(currentNode, true);
        // ---------------------------------------------------

        if (ipDefault.equals(currentNode.getIp()) && portDefault == currentNode.getPort()) { // Check for default node
            currentNode.setNextNode(currentNodeDTO);
            currentNode.setPreviousNode(currentNodeDTO);
        } else { // If it is not the default node, it has to enter the network
            ChordInternalMessage message = new ChordInternalMessage(MessageType.EnterNode, currentNodeDTO);
            this.clientHandler.startClient(ipDefault, portDefault, message, true, usernameDefault);
        }
        InterfaceHandler.success("Node entered the network successfully."); 
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

    public NodeClientHandler getClientHandler() {
        return clientHandler;
    }

    public NodeServerHandler getServerHandler() {
        return serverHandler;
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
                byte[] messageEncryp = EncryptionHandler.encryptWithPubK(message, reciverNode.getPubK());
                UserMessage userMessage = new UserMessage(MessageType.SendMsg, currentNodeDTO, reciverHash, messageEncryp);
                this.clientHandler.startClient(reciverNode.getIp(), reciverNode.getPort(), userMessage, false, reciverNode.getUsername());
            } else { // Reciver is not on the finger table so we have to find its pubK
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