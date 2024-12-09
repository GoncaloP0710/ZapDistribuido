package psd.group4.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Arrays;

import psd.group4.utils.Utils;
import psd.group4.client.MessageEntry;
import psd.group4.client.Node;
import psd.group4.client.UserService;
import psd.group4.dtos.*;
import psd.group4.events.*;
import psd.group4.message.*;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.security.cert.Certificate;
import java.security.Signature;

import cn.edu.buaa.crypto.access.parser.ParserUtils;
import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;

import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;

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

    // Fase 2 - Grupos --------------------------------------------
    private ConcurrentHashMap<BigInteger, String> groupNames = new ConcurrentHashMap<>();
    private ConcurrentHashMap<BigInteger, String[]> groupAtributes = new ConcurrentHashMap<>();
    private ConcurrentHashMap<BigInteger, int[][]> groupAccessPolicy = new ConcurrentHashMap<>();
    private ConcurrentHashMap<BigInteger, PairingKeySerParameter> groupMasterKeys = new ConcurrentHashMap<>();
    private ConcurrentHashMap<BigInteger, PairingKeySerParameter> groupPublicKeys = new ConcurrentHashMap<>();
    private ConcurrentHashMap<BigInteger, PairingKeySerParameter> groupSecretKeys = new ConcurrentHashMap<>();
    private ConcurrentHashMap<BigInteger, String[]> groupRhos = new ConcurrentHashMap<>();
    private ConcurrentHashMap<BigInteger, PairingParameters> groupPairingParameters = new ConcurrentHashMap<>();


    public EventHandler(UserService userService) {
        this.userService = userService;
        ipDefault = userService.getIpDefault();
        portDefault = userService.getPortDefault();
        isDefaultNode = userService.getCurrentNode().checkDefaultNode(ipDefault, portDefault);
        currentNodeDTO = userService.getCurrentNodeDTO();
        currentNode = userService.getCurrentNode();
        clientHandler = userService.getClientHandler();
    }

    /**
     * Update the neighbors of the current node
     * 
     * @param event
     */
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

    /**
     * Handles the enter node event
     * 
     * @param event
     * @throws NoSuchAlgorithmException
     * @throws InterruptedException
     */
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
                clientHandler.startClient(nodeToEnterDTO.getIp(), nodeToEnterDTO.getPort(), new ChordInternalMessage(MessageType.broadcastUpdateFingerTable, false, currentNodeDTO, currentNodeDTO, false), true, nodeToEnterDTO.getUsername());
            } else { // foward to the closest node in the finger table of the current node to the new node
                clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage(), true, nodeWithHashDTO.getUsername());
            }
        } finally {
            enterNodeLock.unlock();
        }
    }

    /**
     * Handles the node trying to exit the network
     * 
     * @throws NoSuchAlgorithmException
     * @throws InterruptedException
     */
    public synchronized void exitNode() throws Exception {
        NodeDTO prevNodeDTO = currentNode.getPreviousNode();
        NodeDTO nextNodeDTO = currentNode.getNextNode();
 
        if (prevNodeDTO.equals(currentNodeDTO) && nextNodeDTO.equals(currentNodeDTO)) { // Only one node in the network
            InterfaceHandler.success("Node exited the network successfully");
        } else {
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
            clientHandler.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), new ChordInternalMessage(MessageType.broadcastUpdateFingerTable, false, prevNodeDTO, prevNodeDTO, true), true, nextNodeDTO.getUsername());

            for (BigInteger key : sharedKeys.keySet()) {
                NodeDTO nodeWithHashDTO = userService.getNodeWithHash(key);
                try {
                    clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), new ChordInternalMessage(MessageType.RemoveSharedKey, key, currentNodeDTO), false, nodeWithHashDTO.getUsername());
                } catch (Exception e) {
                    InterfaceHandler.erro("Error removing shared key: " + e.getMessage());
                }
            }
            Thread.sleep(2000);
        }
        Utils.clearSSLProperties();

        File messagesFile = new File("Mensagens/" + currentNodeDTO.getUsername() + "/messages.dat");
        if (messagesFile.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(messagesFile))) {
                List<MessageEntry> deserializedShares = (List<MessageEntry>) ois.readObject();
                MongoDBHandler mongoDBHandler = new MongoDBHandler();
                for (MessageEntry share : deserializedShares) {
                    mongoDBHandler.storeMessage(share);
                }
                mongoDBHandler.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        File userDirectory = new File("Mensagens/" + currentNodeDTO.getUsername());
        if (userDirectory.exists() && userDirectory.isDirectory()) {
            // List all files in the user's directory
            File[] files = userDirectory.listFiles();
            if (files != null) {
                for (File file : files) {
                    // Delete files in the directory
                    if (!file.isDirectory()) {
                        file.delete();
                    }
                }
            }
            // Delete the user's directory after its contents are removed
            userDirectory.delete();
        }
        InterfaceHandler.success("Node exited the network successfully");
    }

    /**
     * Handles the remove shared key event
     * 
     * @param e
     */
    public synchronized void removeSharedKey(RemoveSharedKeyEvent e) {
        if (currentNodeDTO.getHash().equals(e.getTarget())) {
            sharedKeys.remove(e.getInitializer().getHash());
            InterfaceHandler.info("Shared key removed successfully, " + e.getInitializer().getUsername());
        } else {
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(e.getTarget());
            try {
                clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), e.getMessage(), false, nodeWithHashDTO.getUsername());
            } catch (Exception e1) {
                InterfaceHandler.erro("Error removing shared key: " + e1.getMessage());
            }
        }
    }

    /**
     * Handles the update finger table event
     * 
     * @param event
     * @throws NoSuchAlgorithmException
     */
    public synchronized void updateFingerTable(UpdateNodeFingerTableEvent event) throws NoSuchAlgorithmException {
        ChordInternalMessage message = (ChordInternalMessage) event.getMessage();
        int counter = event.getCounter();
        NodeDTO nodeToUpdateDTO = event.getNodeToUpdate(); // Node that started the event
    
        if (currentNodeDTO.equals(nodeToUpdateDTO)) { // Update the finger table of the current node
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

    /**
     * Recives the message and fowards it to all network eventually
     * 
     * @param event
     * @throws NoSuchAlgorithmException
     */
    public synchronized void broadcastMessage(BroadcastUpdateFingerTableEvent event) throws NoSuchAlgorithmException {
        ChordInternalMessage message = new ChordInternalMessage(MessageType.UpdateFingerTable, event.getSenderDto(), 0);
        updateFingerTable(new UpdateNodeFingerTableEvent(message)); 
        if (!event.getInitializer().equals(currentNodeDTO)) { // foward to the next node
            NodeDTO nextNodeDTO = currentNode.getNextNode();
            ((ChordInternalMessage) event.getMessage()).setSenderDto(currentNodeDTO);
            clientHandler.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), event.getMessage(), false, nextNodeDTO.getUsername());
        }
    }

    /**
     * Sends, Recives and fowards the messages between the nodes
     * 
     * @param event
     * @throws Exception
     */
    public synchronized void sendUserMessage(NodeSendMessageEvent event) throws Exception {
        if (currentNodeDTO.getHash().equals(event.getReciver()) || currentNodeDTO.getHash().equals(event.getSenderDTO().getHash())) { // Arrived at a node that needs the shared key
            boolean hasK = waitForSharedK(event.getReciver(), event.getSenderDTO().getHash());
            if (!hasK) 
                return;
        } else { // Foward to the target
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(event.getReciver());
            clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage(), false, nodeWithHashDTO.getUsername());
            return;
        }   
        if (event.getSenderDTO().getHash().equals(currentNodeDTO.getHash())) { // Start of the process (initializer) - In this step the message hasnt been encrypted yet
            byte[] encryptedBytesAut = EncryptionHandler.encryptWithKey(event.getMessageEncryp(), sharedKeys.get(event.getReciver()));
            byte[] hashSigned = getSignature(EncryptionHandler.createMessageHash(encryptedBytesAut), userService.getKeyHandler().getPrivateKey());

            UserMessage internalMsg = (UserMessage) event.getMessage();
            internalMsg.setMessageEncryp(encryptedBytesAut);
            internalMsg.setMessageHash(hashSigned);
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(event.getReciver());
            clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), internalMsg, false, nodeWithHashDTO.getUsername());

        } else { // Reached the target
            byte[] message = event.getMessageEncryp();
            byte[] decryptedBytes = EncryptionHandler.decryptWithKey(message, sharedKeys.get(event.getSenderDTO().getHash()));
            byte[] hashSigned = event.getMessageHash();

            NodeDTO Sender = event.getSenderDTO();
            checkCertificate(Sender);

            // Verify the signature
            boolean isRightSignature = Utils.verifySignature(userService.getKeyHandler().getTruStore().getCertificate(Sender.getUsername()).getPublicKey(), message, hashSigned);
            if (!isRightSignature) {
                InterfaceHandler.erro("Message signature does not match! Or the hash was altered");
                return;
            }

            String messageString = new String(decryptedBytes, StandardCharsets.UTF_8);
            InterfaceHandler.messageRecived("from " + event.getSenderDTO().getUsername() + ": " + messageString);


            // Encrypt the message using secret sharing
            byte[] sender = Utils.serialize(event.getSenderDTO());
            byte[] receiver = Utils.serialize(currentNodeDTO);
            BigInteger messageDB = messageString.getBytes(StandardCharsets.UTF_8).length > 0 ? new BigInteger(messageString.getBytes(StandardCharsets.UTF_8)) : BigInteger.ZERO;

            List<MessageEntry> shares = EncryptionHandler.divideShare(messageDB, sender, receiver, 1, 3);

            // Create the "Mensagens" directory if it doesn't exist
            String userDir = "Mensagens/" + currentNodeDTO.getUsername();
            Utils.createDir(userDir);

            // Serialize and save the shares to a file
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(userDir + "/messages.dat"))) {
                oos.writeObject(shares);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String recivedMessage = "recived by " + currentNodeDTO.getUsername();
            if (event.getNeedConfirmation()) { // Send a reciving message to the sender
                UserMessage userMessage = new UserMessage(MessageType.SendMsg, currentNodeDTO, event.getSenderDTO().getHash(), recivedMessage.getBytes(), false, (byte[]) null, false);
                NodeSendMessageEvent e = new NodeSendMessageEvent(userMessage);
                sendUserMessage(e);
            }
        }
    }

    /**
     * Handles the event of adding a certificate to the truststore as well as the Diffie-Hellman key exchange to make the process secure
     * 
     * @param e
     * @throws Exception
     */
    public void addCertificateToTrustStore(AddCertificateToTrustStoreEvent e) throws Exception {   
        if (currentNodeDTO.getUsername().equals(e.getAliasSender()) && e.getTargetPublicKey() == null) { // First time on the initializer
            
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
            
            PrivateKey privK = myPrivKeysDiffie.get(e.getInitializer().getHash());
            myPrivKeysDiffie.remove(e.getInitializer().getHash()); // Remove from the shared memory
            byte[] sharedKey = Utils.computeSKey(privK, e.getTargetPublicKey());
            sharedKeys.put(e.getInitializer().getHash(), sharedKey);
            
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

    /**
     * Handles the Diffie-Hellman key exchange between the nodes to make the process of sending messages secure
     * 
     * @param e
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     */
    public synchronized void diffieHellman (DiffHellmanEvent e) throws NoSuchAlgorithmException, InvalidKeyException {
        
        NodeDTO closestNodeToTarget = null;
        if (e.getTargetPublicKey() == null && !currentNodeDTO.getHash().equals(e.getTarget())) { // Foward to Target
            InterfaceHandler.info("Foward to Target - Diffie-Hellman");
            closestNodeToTarget = userService.getNodeWithHash(e.getTarget());
        } else { // Foward to Initializer
            InterfaceHandler.info("Foward to Initializer - Diffie-Hellman");
            closestNodeToTarget = userService.getNodeWithHash(e.getInitializer().getHash());
        }
        
        if (currentNodeDTO.getHash().equals(e.getInitializer().getHash()) && e.getTargetPublicKey() == null) { // First time on the initializer
            InterfaceHandler.info("Starting Diffie-Hellman key exchange");
            KeyPair keypair = Utils.generateKeyPair();
            PrivateKey privK = keypair.getPrivate();
            myPrivKeysDiffie.put(e.getTarget(), privK); // Save on the shared memory for later use
            ChordInternalMessage msg = (ChordInternalMessage) e.getMessage();
            msg.setInitializerPublicKey(keypair.getPublic());
            
            if(closestNodeToTarget == null) {
                InterfaceHandler.erro("Node not found");
                return;
            }
            // foward to the target
            clientHandler.startClient(closestNodeToTarget.getIp(), closestNodeToTarget.getPort(), msg, false, closestNodeToTarget.getUsername());
            return;
            
        } else if (currentNodeDTO.getHash().equals(e.getInitializer().getHash())) { // Second time on the initializer (last stop)
            InterfaceHandler.info("Second time on the initializer - Diffie-Hellman key exchange");
            PrivateKey privK = myPrivKeysDiffie.get(e.getTarget());
            myPrivKeysDiffie.remove(e.getTarget()); // Remove from the shared memory
            byte[] sharedKey = Utils.computeSKey(privK, e.getTargetPublicKey());
            sharedKeys.put(e.getTarget(), sharedKey); // Now both users have the shared key
            notifyAll();
            return;
        
        } else if (currentNodeDTO.getHash().equals(e.getTarget())) { // Reached the target for the first time (only time)
            InterfaceHandler.info("Reached the target for the first time - Diffie-Hellman key exchange");
            KeyPair keypair = Utils.generateKeyPair();
            PrivateKey privK = keypair.getPrivate();
            byte[] sharedKey = Utils.computeSKey(privK, e.getInitializerPublicKey());
            sharedKeys.put(e.getInitializer().getHash(), sharedKey);
            notifyAll();

            ChordInternalMessage msg = (ChordInternalMessage) e.getMessage();
            msg.setTargetPublicKey(keypair.getPublic());

            if(closestNodeToTarget == null) {
                InterfaceHandler.erro("Node not found");
                return;
            }

            // foward to initializer
            clientHandler.startClient(closestNodeToTarget.getIp(), closestNodeToTarget.getPort(), msg, false, closestNodeToTarget.getUsername());
            return;
        }
        
        if(closestNodeToTarget == null) {
            InterfaceHandler.erro("Node not found");
            return;
        }
        clientHandler.startClient(closestNodeToTarget.getIp(), closestNodeToTarget.getPort(), e.getMessage(), false, closestNodeToTarget.getUsername());
    }

    /**
     * Handles the notify event
     * 
     * @param e
     */
    public void handleNotify(NotifyEvent e) {
        if (currentNodeDTO.getHash().equals(e.getTarget().getHash())) {
            synchronized (locker) {
                locker.notify(); // Notify the waiting thread
            }
        } else {
            try {
                NodeDTO nodeWithHashDTO = userService.getNodeWithHash(e.getTarget().getHash());
                if (nodeWithHashDTO != null) {
                    clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), e.getMessage(), false, nodeWithHashDTO.getUsername());
                    return;
                }
                clientHandler.startClient(e.getTarget().getIp(), e.getTarget().getPort(), e.getMessage(), false, e.getTarget().getUsername());
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void sendGroupMessage(NodeSendGroupMessageEvent event) throws Exception {
        if (groupNames.get(event.getGroupNameHash()) == null) {
            InterfaceHandler.internalInfo("Arrived a group message that the user does not belong to");
        } else {
            NodeDTO sender = event.getSenderDTO();
            checkCertificate(sender);

            // Verify the signature
            boolean isRightSignature = Utils.verifySignature(userService.getKeyHandler().getTruStore().getCertificate(sender.getUsername()).getPublicKey(), event.getMessageEncryp(), event.getMessageHash());
            if (!isRightSignature) {
                InterfaceHandler.erro("Message signature does not match! Or the hash was altered");
                return;
            }

            // Decrypt the message
            PairingKeySerParameter publicKey = groupPublicKeys.get(event.getGroupNameHash());
            PairingKeySerParameter secretKey = groupSecretKeys.get(event.getGroupNameHash());
            String[] attributes = groupAtributes.get(event.getGroupNameHash());
            PairingCipherSerParameter ciphertext = Utils.deserialize(event.getMessageEncryp(), PairingCipherSerParameter.class);
            String msg = EncryptionHandler.decryptGroupMessage(publicKey, secretKey, attributes, ciphertext);
            InterfaceHandler.messageRecived("from " + event.getSenderDTO().getUsername() + ": " + msg);
        }
        if (!event.getSenderDTO().equals(currentNodeDTO)) { // foward to the next node
            NodeDTO nextNodeDTO = currentNode.getNextNode();
            clientHandler.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), event.getMessage(), false, nextNodeDTO.getUsername());
        }
    }

    public void createGroup(String groupName) throws PolicySyntaxException, NoSuchAlgorithmException {
        String policy = Utils.generateRandomPolicy();
        String[] attributes = Utils.generateAttributesForPolicy(policy);

        // Setup
        KPABEEngine engine = KPABEGPSW06aEngine.getInstance();

        // Step 2: Generate pairing parameters using TypeACurveGenerator
        int rBits = 160; // Number of bits for the order of the curve
        int qBits = 512; // Number of bits for the field size
        TypeACurveGenerator curveGenerator = new TypeACurveGenerator(rBits, qBits);
        PairingParameters pairingParameters = curveGenerator.generate();

        // Key generation - done by the PKG
        PairingKeySerPair keyPair = engine.setup(pairingParameters, 50); // Setup with 50 attributes (0 to 49)
        PairingKeySerParameter publicKey = keyPair.getPublic();
        PairingKeySerParameter masterKey = keyPair.getPrivate();
        int[][] accessPolicy = ParserUtils.GenerateAccessPolicy(policy);
        String[] rhos = ParserUtils.GenerateRhos(policy);
        PairingKeySerParameter secretKey = engine.keyGen(publicKey, masterKey, accessPolicy, rhos);

        BigInteger groupIndex = groupNameSpecialHash(groupName);
        groupNames.put(groupIndex, groupName);
        groupAtributes.put(groupIndex, attributes);
        groupAccessPolicy.put(groupIndex, accessPolicy);
        groupMasterKeys.put(groupIndex, masterKey);
        groupPublicKeys.put(groupIndex, publicKey);
        groupSecretKeys.put(groupIndex, secretKey);
        groupRhos.put(groupIndex, rhos);
        groupPairingParameters.put(groupIndex, pairingParameters);
        InterfaceHandler.success("Group created successfully, its index is: " + groupIndex);
    }

    public synchronized void addMemberToGroup(AddUserToGroupEvent event) throws Exception {
        if (currentNodeDTO.getHash().equals(event.getReceiverHash()) || currentNodeDTO.getHash().equals(event.getSenderDTO().getHash())) { // Arrived at a node that needs the shared key
            boolean hasK = waitForSharedK(event.getReceiverHash(), event.getSenderDTO().getHash());
            if (!hasK) 
                return;
        }
        if (currentNodeDTO.getHash().equals(event.getReceiverHash())) { // Reached the target
            byte[] groupAtributesDTOBytesEncrypted = event.getGroupAtributesDTOBytesEncrypted();
            byte[] hashSigned = event.getInfoHash();
            byte[] decryptedBytes = EncryptionHandler.decryptWithKey(groupAtributesDTOBytesEncrypted, sharedKeys.get(event.getSenderDTO().getHash()));
            NodeDTO sender = event.getSenderDTO();
            checkCertificate(sender);
            
            boolean isRightSignature = Utils.verifySignature(userService.getKeyHandler().getTruStore().getCertificate(sender.getUsername()).getPublicKey(), groupAtributesDTOBytesEncrypted, hashSigned);
            if (!isRightSignature) { // Verify the signature
                InterfaceHandler.erro("Message signature does not match! Or the hash was altered");
                return;
            }

            boolean userAdded = handleNewUserToGroupParams(decryptedBytes, event.getPublicKey());
            String recivedMessage = currentNodeDTO.getUsername() + " entered the group";
            if (!userAdded) {
                recivedMessage = currentNodeDTO.getUsername() + " already belongs to a group with the same name";
                InterfaceHandler.erro("User not added to group because it already belongs to a group with the same name");
            } else {
                InterfaceHandler.success("User added to group successfully");
            }
            
            UserMessage userMessage = new UserMessage(MessageType.SendMsg, currentNodeDTO, event.getSenderDTO().getHash(), recivedMessage.getBytes(), false, (byte[]) null, false);
            NodeSendMessageEvent e = new NodeSendMessageEvent(userMessage);
            sendUserMessage(e);
        } else {
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(event.getReceiverHash());
            if (nodeWithHashDTO != null) { // Node exists
                if (currentNodeDTO == event.getSenderDTO()) { // Start of the process - Encrypt the critical information
                    byte[] groupAtributesDTOBytesEncrypted = event.getGroupAtributesDTOBytesEncrypted();
                    byte[] encryptedBytesAut = EncryptionHandler.encryptWithKey(groupAtributesDTOBytesEncrypted, sharedKeys.get(event.getReceiverHash()));
                    byte[] hash = EncryptionHandler.createMessageHash(encryptedBytesAut);
                    byte[] hashSigned = getSignature(hash, userService.getKeyHandler().getPrivateKey());
        
                    event.setGroupAtributesDTOBytesEncrypted(encryptedBytesAut);
                    event.setInfoHash(hashSigned);
                    ChordInternalMessage msg = (ChordInternalMessage) event.getMessage();
                    msg.setGroupAtributesDTOBytes(encryptedBytesAut);
                    msg.setInfoHash(hashSigned);

                    clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), msg, false, nodeWithHashDTO.getUsername());
                } else {
                    clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), event.getMessage(), false, nodeWithHashDTO.getUsername());
                }
            } else { // Node does not exist - Send a message to the sender saying that the node does not exist
                String recivedMessage = "Node does not exist";
                UserMessage userMessage = new UserMessage(MessageType.SendMsg, currentNodeDTO, event.getSenderDTO().getHash(), recivedMessage.getBytes(), false, (byte[]) null, false);
                NodeSendMessageEvent e = new NodeSendMessageEvent(userMessage);
                sendUserMessage(e);
                return;
            }
        }
    }

    // ======================================================================================================
    //
    //                                          Auxiliary methods
    //
    // ======================================================================================================

    private BigInteger groupNameSpecialHash(String groupName) throws NoSuchAlgorithmException {
        String userName = currentNodeDTO.getUsername();
        String specialHash = groupName + userName + System.currentTimeMillis();
        return Utils.calculateHash(specialHash);
    }

    /**
     * Makes the signature of the bytes received (hash)
     * 
     * @param hash
     * @param privK
     * @return
     * @throws Exception
     */
    public byte[] getSignature(byte[] hash, PrivateKey privK) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privK);
        signature.update(hash);
        byte[] digitalSignature = signature.sign();
        return digitalSignature;
    }

    /**
     * Add a message to the ConcurrentHashMap messages
     * 
     * @param target
     * @param message
     */
    public void addMessage(BigInteger target, byte[] message) {
        messages.put(target, message);
    }

    public byte[] getSharedKey(BigInteger name) {
        return sharedKeys.get(name); 
    }

    private boolean waitForSharedK(BigInteger reciverHash, BigInteger senderHash) {
        long startTime = System.currentTimeMillis();
        while (sharedKeys.get(reciverHash) == null && sharedKeys.get(senderHash) == null) {
           try {
                wait(2000); // Wait for 1 second intervals
                long elapsedTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
                if (elapsedTime >= TIMEOUT) {
                    InterfaceHandler.erro("Connection timeout! - Shared key not found");
                    return false;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                InterfaceHandler.erro("Thread interrupted: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    private void checkCertificate(NodeDTO nodeDTO) throws Exception {
        if (!userService.getKeyHandler().getTruStore().containsAlias(nodeDTO.getUsername())) {// If the certificate of the other node is not in the truststore
            userService.getClientHandler().shareCertificateClient(nodeDTO.getIp(), nodeDTO.getPort(), new ChordInternalMessage(MessageType.addCertificateToTrustStore, (byte[]) null, (byte[]) null, currentNodeDTO.getUsername(), nodeDTO.getUsername(), currentNodeDTO, (PublicKey) null, (PublicKey) null), nodeDTO.getUsername());
            Thread.sleep(500);
        }
    }

    private boolean handleNewUserToGroupParams(byte[] decryptedBytes, PairingKeySerParameter publicKey) throws Exception {
        GroupAtributesDTO groupAtributesDTO = Utils.deserialize(decryptedBytes, GroupAtributesDTO.class);
        String groupName = groupAtributesDTO.getGroupName();

        if (groupNames.get(groupAtributesDTO.getGroupNameHash()) != null) 
            return false;

        String[] attributes = groupAtributesDTO.getAttributes();
        int[][] accessPolicy = groupAtributesDTO.getAccessPolicy();
        PairingKeySerParameter masterKey = groupAtributesDTO.getMasterKey();
        String[] rhos = groupAtributesDTO.getRhos();
        PairingParameters pairingParameters = groupAtributesDTO.getPairingParameters();

        groupNames.put(groupAtributesDTO.getGroupNameHash(), groupName);
        groupAtributes.put(groupAtributesDTO.getGroupNameHash(), attributes);
        groupRhos.put(groupAtributesDTO.getGroupNameHash(), rhos);
        groupPublicKeys.put(groupAtributesDTO.getGroupNameHash(), publicKey);
        groupMasterKeys.put(groupAtributesDTO.getGroupNameHash(), masterKey);
        groupAccessPolicy.put(groupAtributesDTO.getGroupNameHash(), accessPolicy);
        groupPairingParameters.put(groupAtributesDTO.getGroupNameHash(), pairingParameters);

        KPABEEngine engine = KPABEGPSW06aEngine.getInstance();
        PairingKeySerParameter secretKey = engine.keyGen(publicKey, masterKey, accessPolicy, rhos);
        groupSecretKeys.put(groupAtributesDTO.getGroupNameHash(), secretKey);
        return true;
    }
            
    // TODO: Try protected

    public String[] getGroupAttributes(BigInteger groupNameHash) {
        return groupAtributes.get(groupNameHash);
    }

    public int[][] getGroupAccessPolicy(BigInteger groupNameHash) {
        return groupAccessPolicy.get(groupNameHash);
    }

    public PairingKeySerParameter getGroupMasterKey(BigInteger groupNameHash) {
        return groupMasterKeys.get(groupNameHash);
    }

    public PairingKeySerParameter getGroupPublicKey(BigInteger groupNameHash) {
        return groupPublicKeys.get(groupNameHash);
    }

    public PairingKeySerParameter getGroupSecretKey(BigInteger groupNameHash) {
        return groupSecretKeys.get(groupNameHash);
    }

    public String[] getGroupRhos(BigInteger groupNameHash) {
        return groupRhos.get(groupNameHash);
    }

    public PairingParameters getGroupPairingParameters(BigInteger groupNameHash) {
        return groupPairingParameters.get(groupNameHash);
    }

    public Collection<String> getAllGroupNames() {
        return groupNames.values();
    }

    public BigInteger getKeyByValue(String value) {
        for (Map.Entry<BigInteger, String> entry : groupNames.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null; // Return null if the value is not found
    }

    public ConcurrentHashMap<BigInteger, String> getGroupNames() {
        return groupNames;
    }

    public Collection<BigInteger> getGroupHashesWithSameName(String groupName) {
        List<BigInteger> hashes = new ArrayList<>();
        for (Map.Entry<BigInteger, String> entry : groupNames.entrySet()) {
            if (entry.getValue().equals(groupName)) {
                hashes.add(entry.getKey());
            }
        }
        return hashes;
    }
}
