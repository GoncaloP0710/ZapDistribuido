package Handlers;

import java.math.BigInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.security.cert.Certificate;

import Events.*;
import Message.*;
import Client.Node;
import Client.UserService;
import dtos.*;

public class EventHandler { 

    private UserService userService;
    private NodeClientHandler clientHandler;

    String ipDefault;
    int portDefault;
    boolean isDefaultNode;
    NodeDTO currentNodeDTO;
    Node currentNode;

    // private final Lock updateNeighborsLock = new ReentrantLock();
    private final Lock enterNodeLock = new ReentrantLock();

    // ConcurrentHashMap to store NodeDTOs
    private ConcurrentHashMap<BigInteger, byte[]> messages = new ConcurrentHashMap<>();

    public EventHandler(UserService userService) {
        this.userService = userService;
        ipDefault = userService.getIpDefault();
        portDefault = userService.getPortDefault();
        isDefaultNode = userService.getCurrentNode().checkDefaultNode(ipDefault, portDefault);
        currentNodeDTO = userService.getCurrentNodeDTO();
        currentNode = userService.getCurrentNode();
        clientHandler = userService.getClientHandler();
    }

    public synchronized void updateNeighbors(UpdateNeighboringNodesEvent event) {
        if (event.getNext() != null) {
            currentNode.setNextNode(event.getNext());
        }
        if (event.getPrevious() != null) {
            currentNode.setPreviousNode(event.getPrevious());
        }
    }

    public void enterNode(EnterNodeEvent event) {
        enterNodeLock.lock();
        try {
            BigInteger hash = event.getToEnterHash();
            NodeDTO nodeToEnterDTO = event.getToEnter();
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(hash);

            if (nodeWithHashDTO == null) { // target node (Prev to the new Node) is the current node
                // Update the neighbors
                clientHandler.startClient(currentNode.getNextNode().getIp(), currentNode.getNextNode().getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, event.getToEnter()), true, currentNode.getNextNode().getUsername()); // mudar prev do next para o novo node
                clientHandler.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, currentNode.getNextNode(), (NodeDTO) null), true, nodeToEnterDTO.getUsername()); // mudar next do novo node para o next do current
                currentNode.setNextNode(nodeToEnterDTO);// mudar next do current para o novo node
                clientHandler.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, currentNodeDTO), true, nodeToEnterDTO.getUsername()); // mudar prev do novo node para o current
                
                // Update all the finger tables
                clientHandler.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.broadcastUpdateFingerTable, false, currentNodeDTO, currentNodeDTO), true, nodeToEnterDTO.getUsername());

            } else { // foward to the closest node in the finger table of the current node to the new node
                clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage(), false, nodeWithHashDTO.getUsername());
            }
        } finally {
            enterNodeLock.unlock();
        }
    }

    public synchronized void exitNode() {
        NodeDTO prevNodeDTO = currentNode.getPreviousNode();
        NodeDTO nextNodeDTO = currentNode.getNextNode();
        
        // mudar next do prev para o next do current
        ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateNeighbors, nextNodeDTO, (NodeDTO) null);
        clientHandler.startClient(prevNodeDTO.getIp(), prevNodeDTO.getPort(), message, true, prevNodeDTO.getUsername());
        
        // mudar prev do next para o prev do current
        ChordInternalMessage message2 = new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, prevNodeDTO);
        clientHandler.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), message2, true, nextNodeDTO.getUsername());

        // Update all the finger tables | Next e mandas o current
        clientHandler.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), new ChordInternalMessage(MessageType.broadcastUpdateFingerTable, false, prevNodeDTO, prevNodeDTO), true, nextNodeDTO.getUsername());

    }

    public synchronized void updateFingerTable(UpdateNodeFingerTableEvent event) {
        ChordInternalMessage message = (ChordInternalMessage) event.getMessage();
        int counter = event.getCounter();
        NodeDTO nodeToUpdateDTO = event.getNodeToUpdate(); // Node that started the event
    
        if (currentNodeDTO.equals(nodeToUpdateDTO)) { // Update the finger table of the current node
            userService.getCurrentNode().setFingerTable(message.getFingerTable());
            return;
        } else if (counter == userService.getHashLength()) { // No more nodes to add
            clientHandler.startClient(nodeToUpdateDTO.getIp(), nodeToUpdateDTO.getPort(), message, true, nodeToUpdateDTO.getUsername()); // Send the message back to the node that started the event
            return;
        }
    
        BigInteger ringSizeBig = BigInteger.valueOf(userService.getRingSize());
        BigInteger distance = nodeToUpdateDTO.getHash().subtract(currentNodeDTO.getHash()).add(ringSizeBig).mod(ringSizeBig);
        BigInteger twoPowerCounter = BigInteger.valueOf(2).pow(counter);
    
        if (distance.compareTo(twoPowerCounter) >= 0) {
            message.addNodeToFingerTable(currentNodeDTO);
    
            // Skip the next counters if 2^counter is lower than the distance between this and the next node to try
            while (twoPowerCounter.compareTo(distance) <= 0) {
                counter++;
                twoPowerCounter = BigInteger.valueOf(2).pow(counter); // Update twoPowerCounter for the new value of counter
                message.incCounter();
            }
        }
        
        NodeDTO nextNode = currentNode.getNextNode();
        clientHandler.startClient(nextNode.getIp(), nextNode.getPort(), message, false, nextNode.getUsername());
    }
    
    public synchronized void broadcastMessage(BroadcastUpdateFingerTableEvent event) {
        ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateFingerTable, event.getSenderDto(), 0);
        updateFingerTable(new UpdateNodeFingerTableEvent(message)); 

        if (!event.getInitializer().equals(currentNodeDTO)) { // foward to the next node
            NodeDTO nextNodeDTO = currentNode.getNextNode();
            ((ChordInternalMessage) event.getMessage()).setSenderDto(currentNodeDTO);
            clientHandler.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), event.getMessage(), false, nextNodeDTO.getUsername());
        }
    }

    public synchronized void sendUserMessage(NodeSendMessageEvent event) {
        if (event.getReciver().equals(currentNodeDTO.getHash())) { // Arrived at the target
            try {
                byte[] decryptedBytes = EncryptionHandler.decryptWithPrivK(event.getMessageEncryp(), userService.getKeyHandler().getPrivateKey());
                String messageDecrypted = new String(decryptedBytes);
                System.out.println("Message from " + event.getSenderDTO().getUsername() + ": " + messageDecrypted);
            } catch (Exception e) {
                e.printStackTrace();
            } 
        } else { // Send to the target (foward to closest node to the target, in the finger table)
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(event.getReciver());
            clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage(), false, nodeWithHashDTO.getUsername());
        }
    }

    public void recivePubKey(RecivePubKeyEvent event) {
        if (event.getReceiverPubKey() != null && event.getInitializer().equals(currentNodeDTO)) { // Final destination
            if (hasMessageWithTarget(event.getTarget())) {
                byte[] message = messages.get(event.getTarget());
                messages.remove(event.getTarget());
                try {
                    byte[] encryptedBytes = EncryptionHandler.encryptWithPubK(message, event.getReceiverPubKey());
                    sendUserMessage(new NodeSendMessageEvent(new UserMessage(MessageType.SendMsg, currentNodeDTO, event.getTarget(), encryptedBytes)));
                } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException
                        | NoSuchAlgorithmException | NoSuchPaddingException e) {
                    e.printStackTrace();
                }
            }
            
        } else if (event.getTarget().equals(currentNodeDTO.getHash())) { // Send back to the initializer | Arrived at the target
            ChordInternalMessage message = (ChordInternalMessage) event.getMessage();
            message.setReceiverPubKey(currentNodeDTO.getPubK());
            clientHandler.startClient(event.getInitializer().getIp(), event.getInitializer().getPort(), message, false, event.getInitializer().getUsername());
        
        } else { // Send to the target (foward to closest node to the target, in the finger table)
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(event.getTarget());
            if (nodeWithHashDTO == null) {
                System.err.println("Node not found");
                return;
            }
            userService.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage(), false, nodeWithHashDTO.getUsername());
        }
    }

    public void addCertificateToTrustStore(AddCertificateToTrustStoreEvent event) {    
        try {
            // Extract the certificate and alias from the event
            Certificate certificate = event.getCertificate();
            String alias = currentNodeDTO.getUsername().equals(event.getAliasSender()) ? event.getAliasReciver() : event.getAliasSender();
            userService.getKeyHandler().addCertificateToTrustStore(alias, certificate);
        } catch (Exception e) {
            System.err.println("Error adding certificate to trust store: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public void addMessage(BigInteger target, byte[] message) {
        messages.put(target, message);
    }

    // Method to check if a message with the same target exists
    private boolean hasMessageWithTarget(BigInteger target) {
        return this.messages.containsKey(target);
    }
}