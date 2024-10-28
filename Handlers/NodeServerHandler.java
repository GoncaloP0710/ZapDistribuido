package Handlers;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStoreException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import Client.Node;
import Client.NodeThread;
import Client.UserService;
import dtos.NodeDTO;

public class NodeServerHandler {

    UserService userService;

    KeyHandler keyHandler;
    NodeDTO currentNodeDTO;
    File keystoreFile;
    String keystorePassword;
    File truststoreFile;

    public NodeServerHandler(UserService userService) {
        this.userService = userService;
        this.keyHandler = userService.getKeyHandler();
        this.currentNodeDTO = userService.getCurrentNodeDTO();
        this.keystoreFile = keyHandler.getKeystoreFile();
        this.keystorePassword = keyHandler.getKeyStorePassword();
        this.truststoreFile = keyHandler.getTrustStoreFile();
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
                clientSocket = serverSocket.accept();
                InterfaceHandler.info("New Secure connection: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                NodeThread newServerThread = new NodeThread(clientSocket, null, userService);
                newServerThread.start();
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
    }

    public void serverInsecure(Node node) throws IOException, InterruptedException {
        String ip = node.getIp();
        int port = node.getPort()+1;

        // Use normal ServerSocket instead of SSLServerSocket
        ServerSocket sSocket = new ServerSocket(port); //socket novo

        while (true) {
            Socket clientSocket = null; // other node sockets       
            try {
                clientSocket = sSocket.accept();
                InterfaceHandler.info("New connection from: " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                NodeThread newServerThread = new NodeThread(clientSocket, null, userService);
                newServerThread.start();
            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }
        }
    }
}
