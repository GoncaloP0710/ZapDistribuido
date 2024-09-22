package handlers;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import client.Node;
import handlers.NodeThread;

/**
 * This class is responsible for the node comunication on the network
 */
public class NodeService {

    private Node currentNode;

    private static SSLServerSocket serverSocket;
    public static List<NodeThread> activeServerThreads = Collections.synchronizedList(new ArrayList<>()); // Threads that are currently only listening
    private ConcurrentHashMap<String, NodeThread> activeClientThreads; // Threads that are currently only sending messages

    public NodeService(Node currentNode) {
        this.currentNode = currentNode;
    }

    public void startServer() throws IOException {

        System.setProperty("javax.net.ssl.keyStore", currentNode.getKeystoreFile());
        System.setProperty("javax.net.ssl.keyStorePassword", currentNode.getKeystorePassword());
        System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        // Get an SSLSocketFactory from the SSLContext
        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        SSLServerSocket sslServerSocket = (SSLServerSocket) factory.createServerSocket(currentNode.getPort());

        while (true) {
            Socket clientSocket = null; // other node socket
            try {
                clientSocket = serverSocket.accept();
                NodeThread newServerThread = new NodeThread(currentNode, clientSocket);
                activeServerThreads.add(newServerThread);
                newServerThread.start();

            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }

        }
    }

    public void startClient() {

        System.setProperty("javax.net.ssl.keyStore", currentNode.getKeystoreFile());
        System.setProperty("javax.net.ssl.keyStorePassword", currentNode.getKeystorePassword());
        System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        System.setProperty("javax.net.ssl.trustStore", currentNode.getTruststoreFile());
        System.setProperty("javax.net.ssl.trustStorePassword", currentNode.getKeystorePassword());
        System.setProperty("javax.net.ssl.trustStoreType", "JCEKS");

        // -----------------------------------------------------------------------------------
        
       try {
        // Create SSL socket
        SocketFactory factory = SSLSocketFactory.getDefault();
        SSLSocket sslClientSocket = (SSLSocket) factory.createSocket(currentNode.getIp(), 0);

        // Initialize input and output streams
        ObjectInputStream ois = new ObjectInputStream(sslClientSocket.getInputStream());
        ObjectOutputStream oos = new ObjectOutputStream(sslClientSocket.getOutputStream());

        // Load keystore
        KeyStore keystore = KeyStore.getInstance("JCEKS");
        try (InputStream keystoreStream = new FileInputStream(currentNode.getKeystoreFile())) {
            keystore.load(keystoreStream, currentNode.getKeystorePassword().toCharArray());
        }

        // Load truststore
        KeyStore truststore = KeyStore.getInstance("JCEKS");
        try (InputStream truststoreStream = new FileInputStream(currentNode.getTruststoreFile())) {
            truststore.load(truststoreStream, currentNode.getKeystorePassword().toCharArray());
        }

        // Additional client logic here

    } catch (Exception e) {
        System.err.println(e.getMessage());
        System.exit(-1);
    }
        // -----------------------------------------------------------------------------------
    }

}
