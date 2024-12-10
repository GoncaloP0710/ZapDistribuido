package psd.group4.client;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.net.ssl.SSLContext;

import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
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
    private String ipDefault = "192.168.1.79";
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
            if (eventHandler.getSharedKey(reciverHash) == null) { // If the shared key does not exist - create a new one
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
            String groupName = interfaceHandler.getInput();

            if (!eventHandler.getAllGroupNames().contains(groupName)) { // Check if the group exists
                InterfaceHandler.erro("You are not a member of the group. Or the group does not exist.");
                InterfaceHandler.info("Select the group you want to add the user to: ");
                groupName = interfaceHandler.getInput();
                while (!eventHandler.getAllGroupNames().contains(groupName)) {
                    InterfaceHandler.erro("You are not a member of the group. Or the group does not exist.");
                    InterfaceHandler.info("Select the group you want to add the user to: ");
                    groupName = interfaceHandler.getInput();
                }
            }

            BigInteger groupNameHash = eventHandler.getKeyByValue(groupName);
            if (hasMultipleOccurrences(groupName)) {
                groupNameHash = selectGroup(groupName, interfaceHandler);
            }

            System.out.println("Write the message: ");
            String message = interfaceHandler.getInput();
            PairingCipherSerParameter messageEncryp = EncryptionHandler.encryptGroupMessage(groupName, message, 
                PairingFactory.getPairing(eventHandler.getGroupPairingParameters(groupNameHash)), 
                eventHandler.getGroupPublicKey(groupNameHash), eventHandler.getGroupAttributes(groupNameHash));
            byte[] messageEncrypBytes = Utils.serialize(messageEncryp); // Serialize the message to be able to encrypt
            byte[] hash = EncryptionHandler.createMessageHash(messageEncrypBytes);
            byte[] hashSigned = eventHandler.getSignature(hash, getKeyHandler().getPrivateKey());

            UserMessage msg = new UserMessage(MessageType.SendGroupMsg, currentNodeDTO, messageEncrypBytes, hashSigned, groupName, groupNameHash);
            clientHandler.startClient(currentNode.getNextNode().getIp(), currentNode.getNextNode().getPort(), msg, false, currentNode.getNextNode().getUsername());
        } finally {
            nodeSendMessageLock.unlock();
        }
    }

    public void createGroup(InterfaceHandler interfaceHandler) {
        try {
            System.out.println("Select the group name you want to create: ");
            String groupName = interfaceHandler.getInput();
            eventHandler.createGroup(groupName);
        } catch (Exception e) {
            InterfaceHandler.erro("Error creating group: " + e.getMessage());
        }
    }

    public void addUserToGroup(InterfaceHandler interfaceHandler) throws Exception {
        nodeSendMessageLock.lock();
        try {
            Collection<String> groupNames = eventHandler.getAllGroupNames();
            if (groupNames.isEmpty()) {
                InterfaceHandler.info("There are no groups.");
                return;
            }

            System.out.println("Select the group you want to add the user to (or exit to chose another operation): ");
            String groupName = interfaceHandler.getInput();
            if (groupName.equals("exit"))
                return;

            if (!eventHandler.getAllGroupNames().contains(groupName)) { // Check if the group exists
                InterfaceHandler.erro("You are not a member of the group. Or the group does not exist.");
                InterfaceHandler.info("Select the group you want to add the user to: ");
                groupName = interfaceHandler.getInput();
                if (groupName.equals("exit"))
                    return;
                while (!eventHandler.getAllGroupNames().contains(groupName)) {
                    InterfaceHandler.erro("You are not a member of the group. Or the group does not exist.");
                    InterfaceHandler.info("Select the group you want to add the user to: ");
                    groupName = interfaceHandler.getInput();
                    if (groupName.equals("exit"))
                        return;
                }
            }

            BigInteger groupNameHash = eventHandler.getKeyByValue(groupName);
            if (hasMultipleOccurrences(groupName)) {
                groupNameHash = selectGroup(groupName, interfaceHandler);
            }

            System.out.println("Write the user name (or exit to chose another operation): ");
            String userName = interfaceHandler.getInput();
            if (userName.equals("exit"))
                return;
            BigInteger reciverHash = currentNode.calculateHash(userName);

            if (eventHandler.getSharedKey(reciverHash) == null) { // If the shared key does not exist - create a new one
                InterfaceHandler.internalInfo("The shared key does not exist, creating a new one to be able to send message.");
                ChordInternalMessage messageToSend = new ChordInternalMessage(MessageType.diffHellman, currentNodeDTO, reciverHash, (PublicKey) null, (PublicKey) null);
                DiffHellmanEvent diffHellmanEvent = new DiffHellmanEvent(messageToSend);
                eventHandler.diffieHellman(diffHellmanEvent);
            }

            GroupAtributesDTO groupAtributesDTO = new GroupAtributesDTO(eventHandler.getGroupAccessPolicy(groupNameHash), eventHandler.getGroupRhos(groupNameHash), 
                eventHandler.getGroupPairingParameters(groupNameHash), groupName, eventHandler.getGroupAttributes(groupNameHash), eventHandler.getGroupMasterKey(groupNameHash), groupNameHash);
            byte[] groupAtributesDTOBytes = Utils.serialize(groupAtributesDTO);
            ChordInternalMessage messageToSend = new ChordInternalMessage(MessageType.AddUserToGroup, currentNodeDTO, reciverHash, eventHandler.getGroupPublicKey(groupNameHash), groupAtributesDTOBytes, (byte[]) null);
            AddUserToGroupEvent event = new AddUserToGroupEvent(messageToSend);
            eventHandler.addMemberToGroup(event);
        } finally {
            nodeSendMessageLock.unlock();
        }
    }

    private BigInteger selectGroup(String groupName, InterfaceHandler interfaceHandler) {
        Collection<BigInteger> groupIndex = eventHandler.getGroupHashesWithSameName(groupName);
        int counter = 1;
        // Iterate over the collection and print each value
        for (BigInteger hash : groupIndex) {
            InterfaceHandler.info(counter + ") " + groupName + " - " + hash.toString());
            counter++;
        }
        String selectedGroup = interfaceHandler.getInput();
        while (!Utils.isInteger(selectedGroup) || Integer.valueOf(selectedGroup) < 1 || Integer.valueOf(selectedGroup) >= counter) {
            InterfaceHandler.erro("Invalid input. Please enter a number between 1 and " + counter);
            selectedGroup = interfaceHandler.getInput();
        }
        
        return (BigInteger) groupIndex.toArray()[Integer.valueOf(selectedGroup) - 1];
    }

    /**
     * Makes the changes needed to mantain the network correct when a node exits
     * @throws InterruptedException 
     * @throws NoSuchAlgorithmException 
     */
    public void exitNode() {
        try {
            eventHandler.exitNode();
        } catch (Exception e) {
            InterfaceHandler.erro("Error exiting node: " + e.getMessage());
        }
    }

    public void printGroups() {
        Collection<String> groupNames = eventHandler.getAllGroupNames();
        if (groupNames.isEmpty()) {
            InterfaceHandler.info("There are no groups.");
            return;
        }
        ConcurrentHashMap<BigInteger, String> groupNamesAndHash = eventHandler.getGroupNames();
        for (Map.Entry<BigInteger, String> entry : groupNamesAndHash.entrySet()) {
            InterfaceHandler.info(entry.getValue() + " - " + entry.getKey().toString());
        }
    }

    @Override
    public void processEvent(NodeEvent e) { // Forwards the event to the event handler
        try {
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
            } else if (e instanceof DiffHellmanEvent) {
                eventHandler.diffieHellman((DiffHellmanEvent) e);
            } else if (e instanceof NotifyEvent){
                eventHandler.handleNotify((NotifyEvent) e);
            } else if (e instanceof RemoveSharedKeyEvent) {
                eventHandler.removeSharedKey((RemoveSharedKeyEvent) e);
            } else if (e instanceof NodeSendGroupMessageEvent) {
                eventHandler.sendGroupMessage((NodeSendGroupMessageEvent) e);
            } else if (e instanceof AddUserToGroupEvent) {
                eventHandler.addMemberToGroup((AddUserToGroupEvent) e);
            } else {
                System.out.println("Exception class: " + e.getClass().getName());
                System.out.println("Exception instance: " + e.toString());
                throw new UnsupportedOperationException("Unhandled event type");
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public boolean hasMultipleOccurrences(String groupName) {
        Collection<String> allGroupNames = eventHandler.getAllGroupNames();
        Map<String, Integer> nameCountMap = new HashMap<>();

        for (String name : allGroupNames) {
            nameCountMap.put(name, nameCountMap.getOrDefault(name, 0) + 1);
        }

        return nameCountMap.getOrDefault(groupName, 0) > 1;
    }


    public void printMessages() throws ClassNotFoundException, IOException {

        Utils.clearSSLProperties();

        MongoDBHandler mh = new MongoDBHandler();
        byte[] user = currentNodeDTO.getUsername().getBytes();
        ArrayList<MessageEntry> list = mh.findAllbyUser(user);
        System.out.println(list);
        ArrayList<MessageEntry> list2 = new ArrayList<>();
        System.out.println(list2);
        long i = 0;
   
        for (MessageEntry messageEntry : list) {
            if (i == 0) {
                i = messageEntry.getIdentifier();
                list2.add(messageEntry);
            } else if (i == messageEntry.getIdentifier()) {
                list2.add(messageEntry);
            } else {

                try {
                    // // Deserialize sender and receiver
                    // NodeDTO senderNode = Utils.deserialize(messageEntry.getSender(), NodeDTO.class);
                    // NodeDTO receiverNode = Utils.deserialize(messageEntry.getReceiver(), NodeDTO.class);
                
                    // Extract their usernames
                    String senderUsername = "Gajo1";//senderNode.getUsername();
                    String receiverUsername = "Gajo2";//receiverNode.getUsername();
                
                    // Reconstruct and decode the message
                    String messageContent = new String(EncryptionHandler.reconstructSecret(list2).toByteArray(), StandardCharsets.UTF_8);
                
                    // Build and display the messages
                    InterfaceHandler.messageRecived(
                        messageEntry.getDate() + ": " 
                        + senderUsername + " sent a message to " 
                        + receiverUsername + " saying: " 
                        + messageContent
                    );
                    InterfaceHandler.messageRecived("----------------------------------------------------------");
                // } catch (IOException | ClassNotFoundException e) {
                //     // Handle deserialization errors
                //     e.printStackTrace();
                //     InterfaceHandler.messageRecived("Error: Failed to deserialize sender or receiver.");
                } catch (Exception e) {
                    // Handle any other potential errors
                    e.printStackTrace();
                    InterfaceHandler.messageRecived("Error: An unexpected error occurred.");
                }

                
                i = messageEntry.getIdentifier();
                list2.clear();
                list2.add(messageEntry);
            }
        }
    }
}