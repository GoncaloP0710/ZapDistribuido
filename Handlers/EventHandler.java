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
import java.util.concurrent.TimeUnit;
import java.security.cert.Certificate;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;

import Events.*;
import Message.*;
import Utils.Utils;
import Client.Node;
import Client.UserService;
import dtos.*;

public class EventHandler { 

    private static final long TIMEOUT = 3; // 3 seconds
    private UserService userService;

    String ipDefault;
    int portDefault;
    boolean isDefaultNode;
    NodeDTO currentNodeDTO;
    Node currentNode;

    private NodeClientHandler clientHandler;

    private Object locker = new Object();

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

        handleNotify(new NotifyEvent(new ChordInternalMessage(MessageType.Notify, event.getInitializer())));
    }

    public void enterNode(EnterNodeEvent event) throws NoSuchAlgorithmException, InterruptedException {
        enterNodeLock.lock();
        try {
            BigInteger hash = event.getToEnterHash();
            NodeDTO nodeToEnterDTO = event.getToEnter();
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(hash);

            if (nodeWithHashDTO == null) { // target node (Prev to the new Node) is the current node
                // Update the neighbors
                clientHandler.startClient(currentNode.getNextNode().getIp(), currentNode.getNextNode().getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, event.getToEnter(), currentNodeDTO), true, currentNode.getNextNode().getUsername()); // mudar prev do next para o novo node
                synchronized (locker) {
                    locker.wait(); // Wait for notification
                }
                clientHandler.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, currentNode.getNextNode(), (NodeDTO) null, currentNodeDTO), true, nodeToEnterDTO.getUsername()); // mudar next do novo node para o next do current
                synchronized (locker) {
                    locker.wait(); // Wait for notification
                }
                currentNode.setNextNode(nodeToEnterDTO);// mudar next do current para o novo node
                clientHandler.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, currentNodeDTO, currentNodeDTO), true, nodeToEnterDTO.getUsername()); // mudar prev do novo node para o current
                synchronized (locker) {
                    locker.wait(); // Wait for notification
                }

                // Update all the finger tables
                clientHandler.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.broadcastUpdateFingerTable, false, currentNodeDTO, currentNodeDTO), true, nodeToEnterDTO.getUsername());
 

            } else { // foward to the closest node in the finger table of the current node to the new node
                clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage(), true, nodeWithHashDTO.getUsername());
            }
        } finally {
            enterNodeLock.unlock();
        }
    }

    public synchronized void exitNode() throws NoSuchAlgorithmException, InterruptedException {
        NodeDTO prevNodeDTO = currentNode.getPreviousNode();
        NodeDTO nextNodeDTO = currentNode.getNextNode();
        
        // mudar next do prev para o next do current
        ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateNeighbors, nextNodeDTO, (NodeDTO) null, currentNodeDTO);
        clientHandler.startClient(prevNodeDTO.getIp(), prevNodeDTO.getPort(), message, true, prevNodeDTO.getUsername());
        synchronized (locker) {
            locker.wait(); // Wait for notification
        }

        // mudar prev do next para o prev do current
        ChordInternalMessage message2 = new ChordInternalMessage(MessageType.UpdateNeighbors, (NodeDTO) null, prevNodeDTO, currentNodeDTO);
        clientHandler.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), message2, true, nextNodeDTO.getUsername());
        synchronized (locker) {
            locker.wait(); // Wait for notification
        }

        // Update all the finger tables | Next e mandas o current
        clientHandler.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), new ChordInternalMessage(MessageType.broadcastUpdateFingerTable, false, prevNodeDTO, prevNodeDTO), true, nextNodeDTO.getUsername());

        InterfaceHandler.success("Node exited the network successfully");
    }

    public synchronized void updateFingerTable(UpdateNodeFingerTableEvent event) throws NoSuchAlgorithmException {
        ChordInternalMessage message = (ChordInternalMessage) event.getMessage();
        int counter = event.getCounter();
        NodeDTO nodeToUpdateDTO = event.getNodeToUpdate(); // Node that started the event
    
        if (currentNodeDTO.equals(nodeToUpdateDTO)) { // Update the finger table of the current node

            //try { // Wait for 1 second intervals
            //    wait(3000);
            //} catch (InterruptedException e) {
            //    e.printStackTrace();
            //} 

            for (NodeDTO node : userService.getCurrentNode().getFingerTable()) {
                if (!message.getFingerTable().contains(node)) {
                    clientHandler.endConection(node);
                }
            }

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
    
    public synchronized void broadcastMessage(BroadcastUpdateFingerTableEvent event) throws NoSuchAlgorithmException {
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
            long startTime = System.currentTimeMillis();
            while (sharedKeys.get(event.getReciver()) == null && sharedKeys.get(event.getSenderDTO().getHash()) == null) {
               try {
                    wait(1000); // Wait for 1 second intervals
                    long elapsedTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
                    if (elapsedTime >= TIMEOUT) {
                        InterfaceHandler.erro("Connection timeout!");
                        return;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    InterfaceHandler.erro("Thread interrupted: " + e.getMessage());
                    return;
                }
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

    public void addCertificateToTrustStore(AddCertificateToTrustStoreEvent e) throws Exception {   
        if (currentNodeDTO.getUsername().equals(e.getAliasSender()) && e.getTargetPublicKey() == null) { // First time on the initializer
            
            InterfaceHandler.info("First time on the initializer");
            KeyPair keypair = Utils.generateKeyPair();
            PrivateKey privK = keypair.getPrivate();
            myPrivKeysDiffie.put(e.getInitializer().getHash(), privK); // Save on the shared memory for later use

            // foward to the target
            ChordInternalMessage message = (ChordInternalMessage) e.getMessage();
            message.setInitializer(currentNodeDTO);
            message.setInitializerPublicKey(keypair.getPublic());

            clientHandler.shareCertificateClient(e.getInitializer().getIp(), e.getInitializer().getPort(), message, e.getInitializer().getUsername());
            return;
            
        } else if (currentNodeDTO.getUsername().equals(e.getAliasSender())) { // Second time on the initializer
            
            InterfaceHandler.info("Second time on the initializer");
            
            PrivateKey privK = myPrivKeysDiffie.get(e.getInitializer().getHash());
            myPrivKeysDiffie.remove(e.getInitializer().getHash()); // Remove from the shared memory
            byte[] sharedKey = Utils.computeSKey(privK, e.getTargetPublicKey());
            
            // Get the others certificate and decrypt it
            byte[] certificate = e.getCertificateReciver();
            byte[] decryptedBytes = EncryptionHandler.decryptWithKey(certificate, sharedKey);
            Certificate toAdd =  Utils.byteArrToCertificate(decryptedBytes);
            String alias = currentNodeDTO.getUsername().equals(e.getAliasSender()) ? e.getAliasReciver() : e.getAliasSender();
            userService.getKeyHandler().addCertificateToTrustStore(alias, toAdd);
            Utils.loadTrustStore(userService.getKeyHandler().getTruststorePath(), userService.getKeyHandler().getKeyStorePassword());
            
            // foward to the target
            ChordInternalMessage message = (ChordInternalMessage) e.getMessage();
            message.setInitializer(currentNodeDTO);
            Certificate cert = userService.getKeyHandler().getCertificate();
            byte[] cerBytes = cert.getEncoded();
            byte[] enCer = EncryptionHandler.encryptWithKey(cerBytes, sharedKey);
            message.setCertificateInitializer(enCer);
            clientHandler.shareCertificateClient(e.getInitializer().getIp(), e.getInitializer().getPort(), message, e.getInitializer().getUsername()); 
            return;
        
        } else if (e.getInitializerPublicKey() != null && e.getCertificateInitializer() == null) { // Reached the target for the first time 

            InterfaceHandler.info("Reached the target for the first time ");

            KeyPair keypair = Utils.generateKeyPair();
            PrivateKey privK = keypair.getPrivate();
            byte[] sharedKey = Utils.computeSKey(privK, e.getInitializerPublicKey());
            sharedKeys.put(e.getInitializer().getHash(), sharedKey);
            myPrivKeysDiffie.put(e.getInitializer().getHash(), privK); // Save on the shared memory for later use

            ChordInternalMessage message = (ChordInternalMessage) e.getMessage();
            message.setInitializer(currentNodeDTO);
            message.setTargetPublicKey(keypair.getPublic());

            Certificate cert = userService.getKeyHandler().getCertificate();
            byte[] cerBytes = cert.getEncoded();
            byte[] enCer = EncryptionHandler.encryptWithKey(cerBytes, sharedKey);
            message.setCetificateReciver(enCer);
            clientHandler.shareCertificateClient(e.getInitializer().getIp(), e.getInitializer().getPort(), message, e.getInitializer().getUsername());
            return;
        } else {

            InterfaceHandler.info("Reached the target for the second time? Its the else");

            PrivateKey privK = myPrivKeysDiffie.get(e.getInitializer().getHash());
            myPrivKeysDiffie.remove(e.getInitializer().getHash()); // Remove from the shared memory
            byte[] sharedKey = Utils.computeSKey(privK, e.getInitializerPublicKey());
            
            // Get the others certificate and decrypt it
            byte[] certificate = e.getCertificateInitializer();
            byte[] decryptedBytes = EncryptionHandler.decryptWithKey(certificate, sharedKey);
            Certificate toAdd =  Utils.byteArrToCertificate(decryptedBytes);
            String alias = currentNodeDTO.getUsername().equals(e.getAliasSender()) ? e.getAliasReciver() : e.getAliasSender();
            userService.getKeyHandler().addCertificateToTrustStore(alias, toAdd);
            
            Utils.loadTrustStore(userService.getKeyHandler().getTruststorePath(), userService.getKeyHandler().getKeyStorePassword());
        }
    }


























    public synchronized void diffieHellman (DiffHellmanEvent e) throws NoSuchAlgorithmException, InvalidKeyException {
        
        NodeDTO closestNodeToTarget = null;
        if (e.getTargetPublicKey() == null) { // Foward to Target
            closestNodeToTarget = userService.getNodeWithHash(e.getTarget());
        } else { // Foward to Initializer
            closestNodeToTarget = userService.getNodeWithHash(e.getInitializer().getHash());
        }

        if (closestNodeToTarget == null && !(e.getInitializer().equals(currentNodeDTO) && e.getTargetPublicKey() != null)) // If the target does not exist
            return;
        
        if (currentNodeDTO.getHash().equals(e.getInitializer().getHash()) && e.getTargetPublicKey() == null) { // First time on the initializer
            KeyPair keypair = Utils.generateKeyPair();
            PrivateKey privK = keypair.getPrivate();
            myPrivKeysDiffie.put(e.getTarget(), privK); // Save on the shared memory for later use
            ChordInternalMessage msg = (ChordInternalMessage) e.getMessage();
            msg.setInitializerPublicKey(keypair.getPublic());

            // foward to the target
            clientHandler.startClient(closestNodeToTarget.getIp(), closestNodeToTarget.getPort(), msg, false, closestNodeToTarget.getUsername());
            return;
            
        } else if (currentNodeDTO.getHash().equals(e.getInitializer().getHash())) { // Second time on the initializer (last stop)
            PrivateKey privK = myPrivKeysDiffie.get(e.getTarget());
            myPrivKeysDiffie.remove(e.getTarget()); // Remove from the shared memory
            byte[] sharedKey = Utils.computeSKey(privK, e.getTargetPublicKey());
            sharedKeys.put(e.getTarget(), sharedKey); // Now both users have the shared key
            notifyAll();
            return;
        
        } else if (currentNodeDTO.getHash().equals(e.getTarget())) { // Reached the target for the first time (only time)
            KeyPair keypair = Utils.generateKeyPair();
            PrivateKey privK = keypair.getPrivate();
            byte[] sharedKey = Utils.computeSKey(privK, e.getInitializerPublicKey());
            sharedKeys.put(e.getInitializer().getHash(), sharedKey);
            notifyAll();

            ChordInternalMessage msg = (ChordInternalMessage) e.getMessage();
            msg.setTargetPublicKey(keypair.getPublic());

            // foward to initializer
            clientHandler.startClient(closestNodeToTarget.getIp(), closestNodeToTarget.getPort(), msg, false, closestNodeToTarget.getUsername());
            return;
        } 
        clientHandler.startClient(closestNodeToTarget.getIp(), closestNodeToTarget.getPort(), e.getMessage(), false, closestNodeToTarget.getUsername());
    }

    public void addMessage(BigInteger target, byte[] message) {
        messages.put(target, message);
    }

    public byte[] getSharedKey(BigInteger name) {
        return sharedKeys.get(name); 
    }

    public void handleNotify(NotifyEvent e) {
        if (currentNodeDTO.getHash().equals(e.getTarget().getHash())) {
            synchronized (locker) {
                locker.notify(); // Notify the waiting thread
            }
        } else {
            try {
                clientHandler.startClient(e.getTarget().getIp(), e.getTarget().getPort(), e.getMessage(), false, e.getTarget().getUsername());
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
            }
        }
    }
}