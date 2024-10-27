package Handlers;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.concurrent.ConcurrentHashMap;
import java.security.cert.Certificate;
import java.security.Signature;

import Events.*;
import Message.*;
import Utils.Utils;
import Client.Node;
import Client.UserService;
import dtos.*;

public class EventHandler { 

    private UserService userService;

    String ipDefault;
    int portDefault;
    boolean isDefaultNode;
    NodeDTO currentNodeDTO;
    Node currentNode;

    private NodeClientHandler clientHandler;

    // private final Lock updateNeighborsLock = new ReentrantLock();
    private final Lock enterNodeLock = new ReentrantLock();

    // ConcurrentHashMap to store NodeDTOs
    private ConcurrentHashMap<BigInteger, byte[]> messages = new ConcurrentHashMap<>();

    // ConcurrentHashMap to store the pub keys created by this node in the Diffie-Hellman
    private ConcurrentHashMap<BigInteger, PrivateKey> myPrivKeysDiffie = new ConcurrentHashMap<>();

    // ConcurrentHashMap to store the shared keys
    private ConcurrentHashMap<BigInteger, byte[]> sharedKeys = new ConcurrentHashMap<>();

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
            InterfaceHandler.info("Next node updated successfully to " + event.getNext().getUsername());
        }
        if (event.getPrevious() != null) {
            currentNode.setPreviousNode(event.getPrevious());
            InterfaceHandler.info("Previous node updated successfully to " + event.getPrevious().getUsername());
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
                InterfaceHandler.info("Next node updated successfully to " + nodeToEnterDTO.getUsername());
                clientHandler.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, currentNodeDTO), true, nodeToEnterDTO.getUsername()); // mudar prev do novo node para o current
                
                // Update all the finger tables
                clientHandler.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.broadcastUpdateFingerTable, false, currentNodeDTO, currentNodeDTO), true, nodeToEnterDTO.getUsername());

            } else { // foward to the closest node in the finger table of the current node to the new node
                clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage(), true, nodeWithHashDTO.getUsername());
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

        InterfaceHandler.success("Node exited the network successfully");
    }

    public synchronized void updateFingerTable(UpdateNodeFingerTableEvent event) {
        ChordInternalMessage message = (ChordInternalMessage) event.getMessage();
        int counter = event.getCounter();
        NodeDTO nodeToUpdateDTO = event.getNodeToUpdate(); // Node that started the event
    
        if (currentNodeDTO.equals(nodeToUpdateDTO)) { // Update the finger table of the current node
            userService.getCurrentNode().setFingerTable(message.getFingerTable());
            InterfaceHandler.info("Finger table updated successfully");
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

    public synchronized void sendUserMessage(NodeSendMessageEvent event) throws Exception {
        if (currentNodeDTO.getHash().equals(event.getReciver()) || currentNodeDTO.getHash().equals(event.getSenderDTO().getHash())) { // Arrived at a node that needs the shared key
            while (sharedKeys.get(event.getReciver()) == null && sharedKeys.get(event.getSenderDTO().getHash()) == null) {
                wait();
            }
        } else { // Foward to the target
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(event.getReciver());
            clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage(), false, nodeWithHashDTO.getUsername());
            return;
        }   
        if (event.getSenderDTO().getHash().equals(currentNodeDTO.getHash())) { // Start of the process (initializer)
            byte[] message = event.getMessageEncryp(); // In this step the message hasnt been encrypted yet
            byte[] encryptedBytesAut = EncryptionHandler.encryptWithKey(message, sharedKeys.get(event.getReciver()));
            byte[] hash = EncryptionHandler.createMessageHash(encryptedBytesAut);
            byte[] hashSigned = getSignature(hash, userService.getKeyHandler().getPrivateKey());

            UserMessage internalMsg = (UserMessage) event.getMessage();
            internalMsg.setMessageEncryp(encryptedBytesAut);
            internalMsg.setMessageHash(hashSigned);
            
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(event.getReciver());
            clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), internalMsg, false, nodeWithHashDTO.getUsername());

        } else { // Reached the target
            byte[] message = event.getMessageEncryp();
            byte[] decryptedBytes = EncryptionHandler.decryptWithKey(message, sharedKeys.get(event.getSenderDTO().getHash()));
            byte[] hashSigned = event.getMessageHash();

            // Verify the signature
            PublicKey senderPubKey = event.getSenderDTO().getPubK();
            boolean isRightSignature = verifySignature(senderPubKey, message, hashSigned);
            if (!isRightSignature) {
                InterfaceHandler.erro("Message signature does not match!");
                return;
            }
            String messageString = new String(decryptedBytes, StandardCharsets.UTF_8);
            InterfaceHandler.messageRecived("from " + event.getSenderDTO().getUsername() + ": " + messageString);
            
            String recivedMessage = "recived by " + currentNodeDTO.getUsername();
            if (event.getNeedConfirmation()) { // Send a reciving message to the sender
                UserMessage userMessage = new UserMessage(MessageType.SendMsg, currentNodeDTO, event.getSenderDTO().getHash(), recivedMessage.getBytes(), false, (byte[]) null, false);
                NodeSendMessageEvent e = new NodeSendMessageEvent(userMessage);
                sendUserMessage(e);
            }
        }
    }

    private byte[] getSignature(byte[] hash, PrivateKey privK) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privK);
        signature.update(hash);
        byte[] digitalSignature = signature.sign();
        return digitalSignature;
    }

    private boolean verifySignature(PublicKey senderPubKey, byte[] messageEncrypted, byte[] hashSigned) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(senderPubKey);
        signature.update(EncryptionHandler.createMessageHash(messageEncrypted));

         return signature.verify(hashSigned);
    }

    public void addCertificateToTrustStore(AddCertificateToTrustStoreEvent event) {    
        try {
            // Extract the certificate and alias from the event
            Certificate certificate = event.getCertificate();
            String alias = currentNodeDTO.getUsername().equals(event.getAliasSender()) ? event.getAliasReciver() : event.getAliasSender();
            userService.getKeyHandler().addCertificateToTrustStore(alias, certificate);
            InterfaceHandler.info("Certificate added to trust store successfully");
        } catch (Exception e) {
            System.err.println("Error adding certificate to trust store: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public synchronized void diffieHellman (DiffHellmanEvent e) throws NoSuchAlgorithmException, InvalidKeyException {
        if (currentNodeDTO.getHash().equals(e.getInitializer().getHash()) && e.getTargetPublicKey() == null) { // First time on the initializer
            KeyPair keypair = Utils.generateKeyPair();
            PrivateKey privK = keypair.getPrivate();
            myPrivKeysDiffie.put(e.getTarget(), privK); // Save on the shared memory for later use
            ChordInternalMessage msg = (ChordInternalMessage) e.getMessage();
            msg.setInitializerPublicKey(keypair.getPublic());

            // foward to the target
            NodeDTO closestNodeToTarget = userService.getNodeWithHash(e.getTarget());
            clientHandler.startClient(closestNodeToTarget.getIp(), closestNodeToTarget.getPort(), msg, false, closestNodeToTarget.getUsername());
        
        } else if (currentNodeDTO.getHash().equals(e.getInitializer().getHash())) { // Second time on the initializer (last stop)
            PrivateKey privK = myPrivKeysDiffie.get(e.getTarget());
            myPrivKeysDiffie.remove(e.getTarget()); // Remove from the shared memory
            byte[] sharedKey = Utils.computeSKey(privK, e.getTargetPublicKey());
            sharedKeys.put(e.getTarget(), sharedKey); // Now both users have the shared key
            notifyAll();
        
        } else if (currentNodeDTO.getHash().equals(e.getTarget())) { // Reached the target for the first time (only time)
            KeyPair keypair = Utils.generateKeyPair();
            PrivateKey privK = keypair.getPrivate();
            byte[] sharedKey = Utils.computeSKey(privK, e.getInitializerPublicKey());
            sharedKeys.put(e.getInitializer().getHash(), sharedKey);
            notifyAll();

            ChordInternalMessage msg = (ChordInternalMessage) e.getMessage();
            msg.setTargetPublicKey(keypair.getPublic());

            // foward to initializer
            NodeDTO closestNodeToTarget = userService.getNodeWithHash(e.getInitializer().getHash());
            clientHandler.startClient(closestNodeToTarget.getIp(), closestNodeToTarget.getPort(), msg, false, closestNodeToTarget.getUsername());
        
        } else { // Foward to the target or initializer depending on the case
            if (e.getTargetPublicKey() == null) { // Foward to Target
                NodeDTO closestNodeToTarget = userService.getNodeWithHash(e.getTarget());
                clientHandler.startClient(closestNodeToTarget.getIp(), closestNodeToTarget.getPort(), e.getMessage(), false, closestNodeToTarget.getUsername());
            } else { // Foward to Initializer
                NodeDTO closestNodeToTarget = userService.getNodeWithHash(e.getInitializer().getHash());
                clientHandler.startClient(closestNodeToTarget.getIp(), closestNodeToTarget.getPort(), e.getMessage(), false, closestNodeToTarget.getUsername());
            }
        }
    }

    public void addMessage(BigInteger target, byte[] message) {
        messages.put(target, message);
    }

    public byte[] getSharedKey(BigInteger name) {
        return sharedKeys.get(name); 
    }
}