package psd.group4.handlers;

import java.io.File;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import psd.group4.utils.Utils;
import psd.group4.client.*;
import psd.group4.dtos.NodeDTO;
import psd.group4.message.*;

import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;

public class NodeClientHandler {

    UserService userService;
    KeyHandler keyHandler;
    NodeDTO currentNodeDTO;
    File keystoreFile;
    String keystorePassword;
    File truststoreFile;

    // ConcurrentHashMap to store the Threads
    private ConcurrentHashMap<BigInteger, NodeThread> threads = new ConcurrentHashMap<>();

    // ConcurrentHashMap to store the Threads not secure
    private ConcurrentHashMap<BigInteger, NodeThread> threadsNotSecure = new ConcurrentHashMap<>();

    public NodeClientHandler(UserService userService) {
        this.userService = userService;
        this.keyHandler = userService.getKeyHandler();
        this.currentNodeDTO = userService.getCurrentNodeDTO();
        this.keystoreFile = keyHandler.getKeystoreFile();
        this.keystorePassword = keyHandler.getKeyStorePassword();
        this.truststoreFile = keyHandler.getTrustStoreFile();
    }

    public void endConection (NodeDTO node) {
        if (threads.get(node.getHash()) == null) {
            InterfaceHandler.info("There is no conection with: " + node.getUsername());
            return;
        }
        threads.get(node.getHash()).endThread();
        threads.remove(node.getHash());
        InterfaceHandler.success("Ended conection with: " + node.getUsername());
    }
    
    /**
     * Send command to the Node with the respective ip and port
     * 
     * @param ip
     * @param port
     * @param command
     * @throws NoSuchAlgorithmException 
     */
    public void startClient(String ip, int port, Message msg, boolean waitForResponse, String alias) throws NoSuchAlgorithmException {
        BigInteger hashAlias = Utils.calculateHash(alias);
        if (threads.containsKey(hashAlias)) {
            NodeThread thread = threads.get(hashAlias);
            InterfaceHandler.internalInfo(alias + " already has a Secure conection, added the message to the queue");
            thread.addMessage(msg);
            return;
        }
        InterfaceHandler.info("Creating a new Secure conection with: " + alias);

        try {
            if (!keyHandler.getTruStore().containsAlias(alias)) {// If the certificate of the other node is not in the truststore
                shareCertificateClient(ip, port, new ChordInternalMessage(MessageType.addCertificateToTrustStore, (byte[]) null, (byte[]) null, currentNodeDTO.getUsername(), alias, currentNodeDTO, (PublicKey) null, (PublicKey) null), alias);
                Thread.sleep(500);
            }

            System.setProperty("javax.net.ssl.keyStore", keystoreFile.toString());
            System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
            System.setProperty("javax.net.ssl.keyStoreType", "JKS");

            System.setProperty("javax.net.ssl.trustStore", truststoreFile.toString());
            System.setProperty("javax.net.ssl.trustStorePassword", keystorePassword);
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");

            // Create SSL socket
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslClientSocket = (SSLSocket) factory.createSocket(ip, port);

            NodeThread newClientThread = new NodeThread(sslClientSocket, msg, userService);
            newClientThread.start();
            threads.put(hashAlias, newClientThread);
            InterfaceHandler.info("Secure conection with: " + alias + " created");

        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }

    /**
     * Creates a new conection to the node with the given ip and port to share the certificate
     * 
     * @param ip
     * @param port
     * @param msg
     * @throws NoSuchAlgorithmException
     */
    public void shareCertificateClient(String ip, int port, Message msg, String alias) throws NoSuchAlgorithmException {
        try {

            BigInteger hashAlias = Utils.calculateHash(alias);
            if (threadsNotSecure.containsKey(hashAlias)) {
                NodeThread thread = threadsNotSecure.get(hashAlias);
                thread.addMessage(msg);
                InterfaceHandler.info("Insecure conection with: " + alias + " already exists, added the message to the thread: " + msg.getMsgType());
                return;
            }
            InterfaceHandler.info("Creating a new Insecure conection with: " + alias);
            
            // Create normal socket
            Socket clientSocket = new Socket(ip, port+1);
            NodeThread newClientThread = new NodeThread(clientSocket, msg, userService);
            newClientThread.start();
            threadsNotSecure.put(hashAlias, newClientThread);
        } catch (Exception e) {
            InterfaceHandler.erro("Error creating a new conection to share the certificate");
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }
}
