package Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;

import Events.*;
import Handlers.EncryptionHandler;
import Handlers.InterfaceHandler;
import Handlers.KeyHandler;
import Message.*;
import Utils.observer.*;
import Utils.*;

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
                    in.readObject(); // Force processCommand to finish before continuing
                    break;
                case UpdateFingerTable:
                    in.readObject(); // Force processCommand to finish before continuing
                    break;
                case addCertificateToTrustStore: // The Sender also needs to add the reciver certificate to its trust store
                    ChordInternalMessage message = (ChordInternalMessage) msg;
                    message.setCertificate(sendCert());
                    emitEvent(new AddCertificateToTrustStoreEvent((ChordInternalMessage) message));
                    in.readObject(); // Force processCommand to finish before continuing
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
                    out.writeObject("Force processCommand to finish before continuing");
                    break;
                case UpdateFingerTable:
                    out.writeObject("Force processCommand to finish before continuing");
                    break;
                case addCertificateToTrustStore:
                    out.writeObject("Force processCommand to finish before continuing");
                    break;
                default:
                    break;
            }
            endThread();
        } catch (ClassNotFoundException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    private byte[] reciverDiffieHellman() {
        try{
            KeyPair userKeyPair = Utils.generateKeyPair();
            PrivateKey userPrivateKey = userKeyPair.getPrivate();
            PublicKey userPublicKey = userKeyPair.getPublic();
            PublicKey pubK = (PublicKey) in.readObject(); //Exchange public keys
            out.writeObject(userPublicKey);
            byte[] aesKey = Utils.computeSKey(userPrivateKey, pubK); // Compute the shared secret
            return aesKey;
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    private byte[] senderDiffieHellman() {
        try{
            KeyPair userKeyPair = Utils.generateKeyPair();
            PrivateKey userPrivateKey = userKeyPair.getPrivate();
            PublicKey userPublicKey = userKeyPair.getPublic();
            out.writeObject(userPublicKey); //Exchange public keys
            PublicKey pubK = (PublicKey) in.readObject();
            byte[] aesKey = Utils.computeSKey(userPrivateKey, pubK); // Compute the shared secret
            return aesKey;
        } catch(Exception e){
            e.printStackTrace();
            return null;
        }   
    }

    private Certificate reciveCert() { // Diffie-Hellman
        try{
            byte[] aesKey = reciverDiffieHellman(); // Compute the shared secret
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
            byte[] aesKey = senderDiffieHellman(); // Compute the shared secret
            sendEncrypCert(keyHandler, aesKey, out); // Send encrypted certificate
            return reciveEncrypCert(aesKey, in); // Recive encrypted certificate and decrypt it
        } catch(Exception e){
            e.printStackTrace();
        }
        return null;  
    }    

    private void sendEncrypCert(KeyHandler keyHandler, byte[] aesKey, ObjectOutputStream out) throws Exception {
        
        Certificate certificate = keyHandler.getCertificate();
        byte[] cerBytes = certificate.getEncoded();
        byte[] enCer = EncryptionHandler.encryptWithKey(cerBytes, aesKey);
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
        
        byte[] enCer = (byte[]) in.readObject();
        byte[] deCer = EncryptionHandler.decryptWithKey(enCer, aesKey);
        Certificate toAdd =  Utils.byteArrToCertificate(deCer);
        return toAdd;
    }

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
                emitEvent(new NodeSendMessageEvent((UserMessage) messageToProcess));
                break;
            case addCertificateToTrustStore:
                ((ChordInternalMessage) messageToProcess).setCertificate(reciveCert());
                emitEvent(new AddCertificateToTrustStoreEvent((ChordInternalMessage) messageToProcess));
                break;
            case diffHellman:
                emitEvent(new DiffHellmanEvent((ChordInternalMessage) messageToProcess));
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
