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
import Message.*;
import Utils.observer.*;

public class NodeThread extends Thread implements Subject<NodeEvent> {

    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private Message msg;
    private Listener<NodeEvent> listener;

    // If command is null, it means that the thread is a server and its objective is to process the command it receives
    public NodeThread (Socket socket, Message msg, Listener<NodeEvent> listener) {
        this.socket = socket;
        this.msg = msg;
        setListener(listener);
        try {
            System.out.println("Attempting to create output stream...");
            this.out = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("Output stream created successfully.");
        
            System.out.println("Attempting to create input stream...");
            this.in = new ObjectInputStream(socket.getInputStream());
            System.out.println("Input stream created successfully.");
        } catch (Exception e) {
            System.err.println("Error creating input/output streams: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void setListener (Listener<NodeEvent> listener) {
        this.listener = listener;
    }

    /**
     * Function that is called when the thread is started
     */
    public void run() {
        if (msg != null) {
            sendMsg();
        } else {
            reciveMsg();
        }
    }

    private void sendMsg() {
        try {
            out.writeObject(msg);

            switch (msg.getMsgType()) {
                case UpdateNeighbors:
                    System.out.println((String) in.readObject());
                    break;
                case UpdateFingerTable:
                    System.out.println((String) in.readObject());
                    break;
                case addCertificateToTrustStore:
                    ChordInternalMessage message = (ChordInternalMessage) msg;
                    message.setCertificate(sendCert());
                    emitEvent(new AddCertificateToTrustStoreEvent((ChordInternalMessage) message));
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
                    out.writeObject("Neighbors updated");
                    break;
                case UpdateFingerTable:
                    out.writeObject("Finger table update msg recived");
                    break;
                default:
                    break;
            }

            endThread();
        } catch (ClassNotFoundException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }
    }

    private Certificate reciveCert() { // Diffie-Hellman Key Exchange
        try{
            // Step 1: Generate parameters
            int primeLength = 2048;
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(primeLength);
            
            // Step 2: generates  key pair
            KeyPair userKeyPair = keyPairGenerator.generateKeyPair();
            PrivateKey userPrivateKey = userKeyPair.getPrivate();
            PublicKey userPublicKey = userKeyPair.getPublic();
            
            // Step 3: Exchange public keys
            PublicKey pubK = (PublicKey) in.readObject();
            out.writeObject(userPublicKey);
            
            // Step 4: Compute the shared secret
            KeyAgreement userKeyAgreement = KeyAgreement.getInstance("DH");
            userKeyAgreement.init(userPrivateKey);
            userKeyAgreement.doPhase(pubK, true);
            byte[] userSharedSecret = userKeyAgreement.generateSecret();

            //Recebe certificado em byte[]
            byte[] enCer = (byte[]) in.readObject();

            //desencrypta
            EncryptionHandler eh = new EncryptionHandler();
            byte[] deCer = eh.decryptWithKey(enCer, userSharedSecret);

            //transforma em Certificate
            Certificate toAdd =  byteArrToCertificate(deCer);

            return toAdd;
        } catch(Exception e){
            e.printStackTrace();
        }

        return null;
        
    }

    private Certificate sendCert() { // Diffie-Hellman Key Exchange

        try{
            // Step 1: Generate parameters
            int primeLength = 2048;
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DH");
            keyPairGenerator.initialize(primeLength);
            
            // Step 2: generates  key pair
            KeyPair userKeyPair = keyPairGenerator.generateKeyPair();
            PrivateKey userPrivateKey = userKeyPair.getPrivate();
            PublicKey userPublicKey = userKeyPair.getPublic();
            
            // Step 3: Exchange public keys
            out.writeObject(userPublicKey);
            PublicKey pubK = (PublicKey) in.readObject();

            // Step 4: Compute the shared secret
            KeyAgreement userKeyAgreement = KeyAgreement.getInstance("DH");
            userKeyAgreement.init(userPrivateKey);
            userKeyAgreement.doPhase(pubK, true);
            byte[] userSharedSecret = userKeyAgreement.generateSecret();


            //TODO: return e de onde vem o certifido embaixo
            Certificate certificate = null; //De onde vem o certificado deste user????????

            //-------------------------------------------------------
            byte[] cerBytes = certificate.getEncoded();
 
            //encrypta
            EncryptionHandler eh = new EncryptionHandler();
            byte[] enCer = eh.encryptWithKey(cerBytes, userSharedSecret);

            //envia certificado
            out.writeObject(enCer);
            //--------------------------------------------------------
            
            
            Certificate toAdd = null; //????????????????
            return toAdd; // ????????????é suposto devolver o que?
        } catch(Exception e){
            e.printStackTrace();
        }

        return null;
        
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

    public void endThread() {
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
