package psd.group4.handlers;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import psd.group4.utils.Utils;
import psd.group4.client.Node;
import psd.group4.client.UserService;
import psd.group4.dtos.*;
import psd.group4.events.*;
import psd.group4.message.*;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.security.cert.Certificate;
import java.security.Signature;

// ----------------- Bouncy Castle -----------------


import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.crypto.InvalidCipherTextException;

import cn.edu.buaa.crypto.access.parser.ParserUtils;
import cn.edu.buaa.crypto.access.parser.PolicySyntaxException;
import cn.edu.buaa.crypto.algebra.serparams.PairingCipherSerParameter;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerPair;
import cn.edu.buaa.crypto.algebra.serparams.PairingKeySerParameter;
import cn.edu.buaa.crypto.encryption.abe.kpabe.KPABEEngine;
import cn.edu.buaa.crypto.encryption.abe.kpabe.gpsw06a.KPABEGPSW06aEngine;
import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
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

    // ConcurrentHashMap to store the group attributes
    private ConcurrentHashMap<String, String[]> groupAtributes = new ConcurrentHashMap<>();

    // ConcurrentHashMap to store the group attributes
    private ConcurrentHashMap<String, String> groupPolicy = new ConcurrentHashMap<>();
 
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
    public synchronized void exitNode() throws NoSuchAlgorithmException, InterruptedException {
        NodeDTO prevNodeDTO = currentNode.getPreviousNode();
        NodeDTO nextNodeDTO = currentNode.getNextNode();

        if (prevNodeDTO.equals(currentNodeDTO) && nextNodeDTO.equals(currentNodeDTO)) { // Only one node in the network
            InterfaceHandler.success("Node exited the network successfully");
            return;
        }
        
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
            clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), new ChordInternalMessage(MessageType.RemoveSharedKey, key, currentNodeDTO), false, nodeWithHashDTO.getUsername());
        }

        Thread.sleep(2000);
        InterfaceHandler.success("Node exited the network successfully");
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
        if (event.getIsExiting() && sharedKeys.remove(event.getInitializer().getHash()) != null)
            InterfaceHandler.info("Node exited the network successfully");

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
            long startTime = System.currentTimeMillis();
            while (sharedKeys.get(event.getReciver()) == null && sharedKeys.get(event.getSenderDTO().getHash()) == null) {
               try {
                    wait(2000); // Wait for 1 second intervals
                    long elapsedTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
                    if (elapsedTime >= TIMEOUT) {
                        InterfaceHandler.erro("Connection timeout! - Shared key not found");
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

    /**
     * Makes the signature of the bytes received (hash)
     * 
     * @param hash
     * @param privK
     * @return
     * @throws Exception
     */
    private byte[] getSignature(byte[] hash, PrivateKey privK) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privK);
        signature.update(hash);
        byte[] digitalSignature = signature.sign();
        return digitalSignature;
    }

    /**
     * Verifies the signature of the message with the public key of the sender
     * 
     * @param senderPubKey
     * @param messageEncrypted
     * @param hashSigned
     * @return
     * @throws Exception
     */
    private boolean verifySignature(PublicKey senderPubKey, byte[] messageEncrypted, byte[] hashSigned) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(senderPubKey);
        signature.update(EncryptionHandler.createMessageHash(messageEncrypted));

         return signature.verify(hashSigned);
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

    /**
     * Handles the remove shared key event
     * 
     * @param e
     */
    public void removeSharedKey(RemoveSharedKeyEvent e) {
        if (currentNodeDTO.getHash().equals(e.getTarget())) {
            sharedKeys.remove(e.getInitializer().getHash());
        } else {
            NodeDTO nodeWithHashDTO = userService.getNodeWithHash(e.getTarget());
            try {
                clientHandler.startClient(nodeWithHashDTO.getIp(), nodeWithHashDTO.getPort(), e.getMessage(), false, nodeWithHashDTO.getUsername());
            } catch (NoSuchAlgorithmException e1) {
                e1.printStackTrace();
            }
        }
    }

    public void sendGroupMessage(NodeSendGroupMessageEvent event) {
        
        if (groupAtributes.get(event.getGroupName()) == null) {
            InterfaceHandler.internalInfo("Arrived a group message that the user does not belong to");
        } else {
            
        }

        //if (!event.getInitializer().equals(currentNodeDTO)) { // foward to the next node
        //    NodeDTO nextNodeDTO = currentNode.getNextNode();
        //    ((ChordInternalMessage) event.getMessage()).setSenderDto(currentNodeDTO);
        //    clientHandler.startClient(nextNodeDTO.getIp(), nextNodeDTO.getPort(), event.getMessage(), false, nextNodeDTO.getUsername());
        //}
    }


    public void createGroup(String groupName) throws PolicySyntaxException {
        String policy = generateRandomPolicy();
        String[] attributes = generateAttributesForPolicy(policy);
        groupAtributes.put(groupName, attributes);
        groupPolicy.put(groupName, policy);

        // Setup
        KPABEEngine engine = KPABEGPSW06aEngine.getInstance();

        // Step 2: Generate pairing parameters using TypeACurveGenerator
        int rBits = 160; // Number of bits for the order of the curve
        int qBits = 512; // Number of bits for the field size
        TypeACurveGenerator curveGenerator = new TypeACurveGenerator(rBits, qBits);
        PairingParameters pairingParameters = curveGenerator.generate();
        Pairing pairing = PairingFactory.getPairing(pairingParameters);
        System.out.println("Pairing parameters generated and pairing instance created.");

        // Key generation - done by the PKG
        PairingKeySerPair keyPair = engine.setup(pairingParameters, 50); // Setup with 50 attributes (0 to 49)
        PairingKeySerParameter publicKey = keyPair.getPublic();
        PairingKeySerParameter masterKey = keyPair.getPrivate();

        int[][] accessPolicy = ParserUtils.GenerateAccessPolicy(policy);
        String[] rhos = ParserUtils.GenerateRhos(policy);
        PairingKeySerParameter secretKey = engine.keyGen(publicKey, masterKey, accessPolicy, rhos);
    }

    public void addMemberToGroup() {
        // TODO
        // send the pubK
        // send the accessPolicy
        // send the rhos
        // create unique master key
        // create unique secret key
    }




    // -------------- To be moved to another class --------------


    private static String generateRandomPolicy() {
        Random random = new Random();
        int numClauses = random.nextInt(3) + 1; // Number of clauses in the policy
        StringBuilder policy = new StringBuilder();
    
        for (int i = 0; i < numClauses; i++) {
            if (i > 0) {
                policy.append(" and ");
            }
            int numTerms = random.nextInt(2) + 1; // Number of terms in the clause
            if (numTerms == 1) {
                policy.append(random.nextInt(50));
            } else {
                policy.append("(");
                for (int j = 0; j < numTerms; j++) {
                    if (j > 0) {
                        policy.append(" or ");
                    }
                    policy.append(random.nextInt(50));
                }
                policy.append(")");
            }
        }
    
        return policy.toString();
    }
            
    private static String[] generateAttributesForPolicy(String policy) {
        // Extract numbers from the policy
        String[] tokens = policy.split("[^0-9]+");
        return Arrays.stream(tokens)
                .filter(token -> !token.isEmpty())
                .distinct()
                .toArray(String[]::new);
    }

    private void encryptGroupMessage(String groupName, String originalMessage) throws PolicySyntaxException {
        String[] attributes = groupAtributes.get(groupName);
        byte[] messageBytes = originalMessage.getBytes(StandardCharsets.UTF_8);
        Element message = encodeBytesToGroup(pairing, messageBytes);
        PairingCipherSerParameter ciphertext = engine.encryption(publicKey, attributes, message);
    }

    private void decryptGroupMessage(PairingKeySerParameter publicKey, PairingKeySerParameter secretKey, String[] attributes, PairingCipherSerParameter ciphertext) throws InvalidCipherTextException {
        Element decryptedMessage = engine.decryption(publicKey, secretKey, attributes, ciphertext);
        byte[] decryptedBytes = decodeGroupToBytes(decryptedMessage);
        String recoveredMessage = new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    // Encode a byte array to a group element
    private static Element encodeBytesToGroup(Pairing pairing, byte[] data) {
        // Convert the byte array to a BigInteger
        java.math.BigInteger bigInteger = new java.math.BigInteger(1, data);
        return pairing.getGT().newElement(bigInteger).getImmutable();
    }

    // Decode a group element back to a byte array
    private static byte[] decodeGroupToBytes(Element element) {
        // Convert the group element to a BigInteger
        java.math.BigInteger bigInteger = element.toBigInteger();
        // Convert the BigInteger to a byte array
        return bigInteger.toByteArray();
    }
}
