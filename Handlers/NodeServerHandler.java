package Handlers;

import java.io.IOException;
import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import Client.Node;
import Client.NodeThread;
import Client.UserService;
import Utils.Utils;

public class NodeServerHandler {

    UserService userService;
    Node currentNode;
    KeyHandler keyHandler;
    File keystoreFile;
    String keystorePassword;
    File truststoreFile;

    public NodeServerHandler(UserService userService, Node currentNode, KeyHandler keyHandler, File keystoreFile, String keystorePassword, File truststoreFile) {
        this.userService = userService;
        this.currentNode = currentNode;
        this.keyHandler = keyHandler;
        this.keystoreFile = keystoreFile;
        this.keystorePassword = keystorePassword;
        this.truststoreFile = truststoreFile;
    }

    public void startServerInThread(Node node, boolean secure) {
        Runnable serverTask = () -> {
            try {
                if (secure) {
                    startServer(node);
                } else {
                    serverInsecure(node);
                }
            } catch (IOException | KeyStoreException | InterruptedException e) {
                System.err.println("Error starting server: " + e.getMessage());
                e.printStackTrace();
            }
        };
        Thread serverThread = new Thread(serverTask);
        serverThread.start();
    }

    public void startServer(Node node) throws IOException, KeyStoreException {

        System.setProperty("javax.net.ssl.keyStore", keystoreFile.toString());
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", "JKS");

        System.setProperty("javax.net.ssl.trustStore", truststoreFile.toString());
        System.setProperty("javax.net.ssl.trustStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");

        String ip = node.getIp();
        int port = node.getPort();

        // Get an SSLSocketFactory from the SSLContext
        SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port);

        while (true) {
            Socket clientSocket = null; // other node sockets
            try {
                if(serverSocket != null){
                    System.out.println(serverSocket.toString());
                }
                clientSocket = serverSocket.accept();
                NodeThread newServerThread = new NodeThread(clientSocket, null, userService, keyHandler);
                newServerThread.start();
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
    }

    public void serverInsecure(Node node) throws IOException, InterruptedException {
        this.currentNode = node;
        String ip = node.getIp();
        int port = node.getPort()+1;

        System.out.println("Starting server on " + ip + ":" + port);
        // Use normal ServerSocket instead of SSLServerSocket
        ServerSocket sSocket = new ServerSocket(port); //socket novo

        while (true) {
            Socket clientSocket = null; // other node sockets
            try {
                System.err.println("Server socket waiting for connection..." + sSocket.getLocalPort()); //PRINT MVP
                clientSocket = sSocket.accept();
                NodeThread newServerThread = new NodeThread(clientSocket, null, userService, keyHandler);
                newServerThread.start();
                newServerThread.join(); // Wait for the thread to finish
                System.out.println(keyHandler.getTruststorePath());
                Utils.loadTrustStore(keyHandler.getTruststorePath(), keyHandler.getKeyStorePassword());
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
    }
}
