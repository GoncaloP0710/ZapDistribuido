package psd.group4.client;

import java.io.File;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import psd.group4.handlers.*;
import psd.group4.interfaces.UserServiceInterface;
import psd.group4.utils.*;
import psd.group4.dtos.*;
import psd.group4.events.*;
import psd.group4.message.*;

/**
 * This class is responsible for the node comunication on the network
 */
public class UserService implements UserServiceInterface {

   // ---------------------- Default Node ----------------------
    private String ipDefault = "192.168.1.9";
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
        if (distanceToNext > distanceToNode)  // If the node is between the current node and the next node (in these case the node does not exist)
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
     * Handles the user input and calls the respective method to send the message
     * 
     * @param interfaceHandler
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws NoSuchPaddingException
     * @throws Exception
     */
    public void sendMessage(InterfaceHandler interfaceHandler) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, Exception {
        nodeSendMessageLock.lock();
        try {
            System.out.println("Select the user you want to send a message to: ");
            String reciver = interfaceHandler.getInput();
            System.out.println("Write the message: ");
            byte[] message = interfaceHandler.getInput().getBytes();
        
            BigInteger reciverHash = currentNode.calculateHash(reciver);
            if (eventHandler.getSharedKey(reciverHash) == null) { // If the shared key does not exist
                InterfaceHandler.internalInfo("The shared key does not exist, creating a new one to be able to send message.");
                ChordInternalMessage messageToSend = new ChordInternalMessage(MessageType.diffHellman, currentNodeDTO, reciverHash, (PublicKey) null, (PublicKey) null);
                DiffHellmanEvent diffHellmanEvent = new DiffHellmanEvent(messageToSend);
                eventHandler.diffieHellman(diffHellmanEvent);
            }
            UserMessage userMessage = new UserMessage(MessageType.SendMsg, currentNodeDTO, reciverHash, message, true, (byte[]) null, false);
            NodeSendMessageEvent e = new NodeSendMessageEvent(userMessage);
            eventHandler.sendUserMessage(e);
        } finally {
            nodeSendMessageLock.unlock();
        }
    }

    public void sendGroupMessage(InterfaceHandler interfaceHandler) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, Exception {
        nodeSendMessageLock.lock();
        try {
            System.out.println("Select the group you want to send a message to: ");
            String reciver = interfaceHandler.getInput();
            System.out.println("Write the message: ");
            byte[] message = interfaceHandler.getInput().getBytes();
        
            NodeSendGroupMessageEvent event = new NodeSendGroupMessageEvent(new UserMessage(MessageType.SendGroupMsg, currentNodeDTO, message, (byte[]) null, reciver));
            eventHandler.sendGroupMessage(event);
        } finally {
            nodeSendMessageLock.unlock();
        }
    }

    /**
     * Makes the changes needed to mantain the network correct when a node exits
     * @throws InterruptedException 
     * @throws NoSuchAlgorithmException 
     */
    public void exitNode() throws NoSuchAlgorithmException, InterruptedException {
        eventHandler.exitNode();
    }

    @Override
    public void processEvent(NodeEvent e) { // Forwards the event to the event handler
        if (e instanceof EnterNodeEvent) {
            try {
                eventHandler.enterNode((EnterNodeEvent) e);
            } catch (NoSuchAlgorithmException | InterruptedException e1) {
                e1.printStackTrace();
            }
        } else if (e instanceof UpdateNeighboringNodesEvent) {
            eventHandler.updateNeighbors((UpdateNeighboringNodesEvent) e);
        } else if (e instanceof UpdateNodeFingerTableEvent) {
            try {
                eventHandler.updateFingerTable((UpdateNodeFingerTableEvent) e);
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
            }
        } else if (e instanceof BroadcastUpdateFingerTableEvent) {
            try {
                eventHandler.broadcastMessage(((BroadcastUpdateFingerTableEvent) e));
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
            }
        } else if (e instanceof NodeSendMessageEvent) {
            try {
                eventHandler.sendUserMessage(((NodeSendMessageEvent) e));
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } else if (e instanceof AddCertificateToTrustStoreEvent) {
            try {
                eventHandler.addCertificateToTrustStore((AddCertificateToTrustStoreEvent) e);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        } else if (e instanceof DiffHellmanEvent) {
            try {
                eventHandler.diffieHellman((DiffHellmanEvent) e);
            } catch (InvalidKeyException | NoSuchAlgorithmException e1) {
                e1.printStackTrace();
            }
        } else if (e instanceof NotifyEvent){
            eventHandler.handleNotify((NotifyEvent) e);
        } else if (e instanceof RemoveSharedKeyEvent) {
            eventHandler.removeSharedKey((RemoveSharedKeyEvent) e);
        } else {
            System.out.println("Exception class: " + e.getClass().getName());
            System.out.println("Exception instance: " + e.toString());
            throw new UnsupportedOperationException("Unhandled event type");
        }
    }
}