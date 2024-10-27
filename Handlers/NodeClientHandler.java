package Handlers;

import java.io.File;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigInteger;


import Client.*;
import Message.*;
import dtos.NodeDTO;
import Utils.Utils;

public class NodeClientHandler {

    UserService userService;

    KeyHandler keyHandler;
    NodeDTO currentNodeDTO;
    File keystoreFile;
    String keystorePassword;
    File truststoreFile;

    // ConcurrentHashMap to store the Threads
    private ConcurrentHashMap<BigInteger, NodeThread> threads = new ConcurrentHashMap<>();

    public NodeClientHandler(UserService userService) {
        this.userService = userService;
        this.keyHandler = userService.getKeyHandler();
        this.currentNodeDTO = userService.getCurrentNodeDTO();
        this.keystoreFile = keyHandler.getKeystoreFile();
        this.keystorePassword = keyHandler.getKeyStorePassword();
        this.truststoreFile = keyHandler.getTrustStoreFile();
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
            thread.addMessage(msg);
            return;
        }

        try {
            if (!keyHandler.getTruStore().containsAlias(alias)) // If the certificate of the other node is not in the truststore
                shareCertificateClient(ip, port, new ChordInternalMessage(MessageType.addCertificateToTrustStore, keyHandler.getCertificate(alias), alias, currentNodeDTO.getUsername()));

            System.setProperty("javax.net.ssl.keyStore", keystoreFile.toString());
            System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
            System.setProperty("javax.net.ssl.keyStoreType", "JKS");

            System.setProperty("javax.net.ssl.trustStore", truststoreFile.toString());
            System.setProperty("javax.net.ssl.trustStorePassword", keystorePassword);
            System.setProperty("javax.net.ssl.trustStoreType", "JKS");

            // Create SSL socket
            SSLSocketFactory factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
            SSLSocket sslClientSocket = (SSLSocket) factory.createSocket(ip, port);

            NodeThread newClientThread = new NodeThread(sslClientSocket, msg, userService, keyHandler);
            newClientThread.start();
    
            if (waitForResponse) 
                newClientThread.join(); // Wait for the thread to finish
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
    private void shareCertificateClient(String ip, int port, Message msg) throws NoSuchAlgorithmException {
        try {
            // Create normal socket
            Socket clientSocket = new Socket(ip, (port+1));
            NodeThread newClientThread = new NodeThread(clientSocket, msg, userService, keyHandler);
            newClientThread.start();
            newClientThread.join(); // Wait for the thread to finish
            Utils.loadTrustStore(keyHandler.getTruststorePath(), keyHandler.getKeyStorePassword());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }
}
