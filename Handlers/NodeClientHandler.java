package Handlers;

import java.io.File;
import java.net.Socket;
import java.security.*;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.SSLSocket;

import Client.Node;
import Client.NodeThread;
import Client.UserService;
import Utils.Utils;
import dtos.NodeDTO;
import Message.*;

public class NodeClientHandler {
    
    UserService userService;
    Node currentNode;
    NodeDTO currentNodeDTO;
    KeyHandler keyHandler;
    File keystoreFile;
    String keystorePassword;
    File truststoreFile;

    public NodeClientHandler(UserService userService, Node currentNode, NodeDTO currentNodeDTO, KeyHandler keyHandler, File keystoreFile, String keystorePassword, File truststoreFile) {
        this.userService = userService;
        this.currentNode = currentNode;
        this.currentNodeDTO = currentNodeDTO;
        this.keyHandler = keyHandler;
        this.keystoreFile = keystoreFile;
        this.keystorePassword = keystorePassword;
        this.truststoreFile = truststoreFile;
    }

    /**
     * Send command to the Node with the respective ip and port
     * 
     * @param ip
     * @param port
     * @param command
     */
    public void startClient(String ip, int port, Message msg, boolean waitForResponse, String alias) {
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
            System.out.println("Start of shareCertificateClient");
            // Create normal socket
            System.out.println("Creating socket...");
            System.out.println("IP: " + ip + " Port: " + (port+1));
            Socket clientSocket = new Socket(ip, (port+1));
            NodeThread newClientThread = new NodeThread(clientSocket, msg, userService, keyHandler);
            newClientThread.start();
            newClientThread.join(); // Wait for the thread to finish
            System.out.println(keyHandler.getTruststorePath());
            Utils.loadTrustStore(keyHandler.getTruststorePath(), keyHandler.getKeyStorePassword());
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }
}
