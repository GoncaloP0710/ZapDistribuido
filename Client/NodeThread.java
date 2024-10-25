package Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import javax.crypto.KeyAgreement;

import Events.*;
import Handlers.EncryptionHandler;
import Handlers.KeyHandler;
import Message.*;
import Utils.observer.*;

public class NodeThread extends Thread implements Subject<NodeEvent> {

    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private Message msg;
    private Listener<NodeEvent> listener;
    private KeyHandler keyHandler;

    // If msg is null, it means that the thread is a server and its objective is to process the command it receives
    public NodeThread (Socket socket, Message msg, Listener<NodeEvent> listener, KeyHandler keyHandler) {
        this.socket = socket;
        this.msg = msg;
        this.keyHandler = keyHandler;
        this.listener = listener;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.err.println("Error creating input/output streams: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Function that is called when the thread is started
     */
    public void run() {
        if (msg != null) sendMsg(); else reciveMsg();
    }

    public void endThread() {
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMsg() {
        try {
            out.writeObject(msg); // Send the message to the node reciver
            switch (msg.getMsgType()) {
                case UpdateNeighbors:
                    System.out.println((String) in.readObject());
                    break;
                case UpdateFingerTable:
                    in.readObject(); // Force processCommand to finish before continuing
                    break;
                case addCertificateToTrustStore: // The Sender also needs to add the reciver certificate to its trust store
                    ChordInternalMessage message = (ChordInternalMessage) msg;
                    message.setCertificate(sendCert());
                    emitEvent(new AddCertificateToTrustStoreEvent((ChordInternalMessage) message));
                    System.out.println((String) in.readObject());
                    break;
                default:
                    break;
            }
            endThread();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void reciveMsg() {
        try {
            Message messageToProcess = (Message) in.readObject();
            processCommand(messageToProcess);
            switch (messageToProcess.getMsgType()) {
                case UpdateNeighbors:
                    out.writeObject("Info: Neighbors updated");
                    break;
                case UpdateFingerTable:
                    out.writeObject("Force processCommand to finish before continuing");
                    break;
                case addCertificateToTrustStore:
                    out.writeObject("Info: A certificate was added to trust store");
                    break;
                default:
                    break;
            }
            endThread();
        } catch (ClassNotFoundException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    private Certificate reciveCert() { // Diffie-Hellman
        try{
            KeyPair userKeyPair = generateKeyPair();
            PrivateKey userPrivateKey = userKeyPair.getPrivate();
            PublicKey userPublicKey = userKeyPair.getPublic();
            
            PublicKey pubK = (PublicKey) in.readObject(); // Step 3: Exchange public keys
            out.writeObject(userPublicKey);
            byte[] aesKey = computeSKey(userPrivateKey, pubK); // Compute the shared secret

            Certificate toAdd =  reciveEncrypCert(aesKey, in); // Recive encrypted certificate and decrypt it
            sendEncrypCert(keyHandler, aesKey, out); // Send encrypted certificate
            return toAdd;
        } catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    private Certificate sendCert() { // Diffie-Hellman
        try{
            KeyPair userKeyPair = generateKeyPair();
            PrivateKey userPrivateKey = userKeyPair.getPrivate();
            PublicKey userPublicKey = userKeyPair.getPublic();
            
            out.writeObject(userPublicKey); // Step 3: Exchange public keys
            PublicKey pubK = (PublicKey) in.readObject();
            byte[] aesKey = computeSKey(userPrivateKey, pubK); // Compute the shared secret

            sendEncrypCert(keyHandler, aesKey, out); // Send encrypted certificate
            return reciveEncrypCert(aesKey, in); // Recive encrypted certificate and decrypt it
        } catch(Exception e){
            e.printStackTrace();
        }
        return null;  
    }

    // ===================================================================

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        // Step 1: Generate parameters
        int primeLength = 2048;
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
        keyPairGenerator.initialize(primeLength);
        
        // Step 2: generates  key pair
        KeyPair userKeyPair = keyPairGenerator.generateKeyPair();
        return userKeyPair;
    }

    private byte[] computeSKey(PrivateKey userPrivateKey, PublicKey pubK) throws NoSuchAlgorithmException, InvalidKeyException {
        // Step 4: Compute the shared secret
        KeyAgreement userKeyAgreement = KeyAgreement.getInstance("DH");
        userKeyAgreement.init(userPrivateKey);
        userKeyAgreement.doPhase(pubK, true);
        byte[] userSharedSecret = userKeyAgreement.generateSecret();

        // Ensure the shared secret is 256 bits (32 bytes)
        byte[] aesKey = new byte[32];
        System.arraycopy(userSharedSecret, 0, aesKey, 0, Math.min(userSharedSecret.length, 32));
        return aesKey;
    }

    private void sendEncrypCert(KeyHandler keyHandler, byte[] aesKey, ObjectOutputStream out) throws Exception {
        // Step 5: Send encrypted certificate
        Certificate certificate = keyHandler.getCertificate();
        byte[] cerBytes = certificate.getEncoded();
        EncryptionHandler eh = new EncryptionHandler();
        byte[] enCer = eh.encryptWithKey(cerBytes, aesKey);
        out.writeObject(enCer);
    }

    /**
     * Recives the encrypted certificate and decrypts it
     * 
     * @param aesKey
     * @return
     * @throws Exception
     */
    private Certificate reciveEncrypCert(byte[] aesKey, ObjectInputStream in) throws Exception {
        // Step 5: Recive encrypted certificate
        byte[] enCer = (byte[]) in.readObject();
        EncryptionHandler eh = new EncryptionHandler();
        byte[] deCer = eh.decryptWithKey(enCer, aesKey);
        Certificate toAdd =  byteArrToCertificate(deCer);
        return toAdd;
    }

    public Certificate byteArrToCertificate(byte[] bytes){
        try{
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream inStream = new ByteArrayInputStream(bytes);
            Certificate cert = certFactory.generateCertificate(inStream);
            inStream.close();
            return cert;
        } catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    // ===================================================================

    /**
     * Transform the command on a NodeEvent
     * 
     * @param command
     * @return
     * @throws IOException 
     * @throws ClassNotFoundException 
     * @throws NoSuchAlgorithmException 
     */
    public void processCommand(Message messageToProcess) throws ClassNotFoundException, IOException, NoSuchAlgorithmException {
        switch (messageToProcess.getMsgType()) {
            case EnterNode:
                emitEvent(new EnterNodeEvent((ChordInternalMessage) messageToProcess));
                break;
            case UpdateNeighbors:
                emitEvent(new UpdateNeighboringNodesEvent((ChordInternalMessage) messageToProcess));
                break;
            case UpdateFingerTable:
                emitEvent(new UpdateNodeFingerTableEvent((ChordInternalMessage) messageToProcess));
                break;
            case broadcastUpdateFingerTable:
                emitEvent(new BroadcastUpdateFingerTableEvent((ChordInternalMessage) messageToProcess));
                break;
            case SendMsg:
                System.out.println("Recived a send message event...");
                emitEvent(new NodeSendMessageEvent((UserMessage) messageToProcess));
                break;
            case RecivePubKey:
                emitEvent(new RecivePubKeyEvent((ChordInternalMessage) messageToProcess));
                break;
            case addCertificateToTrustStore:
                ((ChordInternalMessage) messageToProcess).setCertificate(reciveCert());
                emitEvent(new AddCertificateToTrustStoreEvent((ChordInternalMessage) messageToProcess));
                break;
            default:
                break;
        }
        return;
    }

    @Override
    public void emitEvent(NodeEvent e) {
        this.listener.processEvent(e);
    }

}
