package psd.group4.handlers;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyStoreException;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import psd.group4.client.Node;
import psd.group4.client.NodeThread;
import psd.group4.client.UserService;
import psd.group4.dtos.NodeDTO;

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
    
    /**
     * Start the server in a new thread
     * 
     * @param node
     * @param secure
     */
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

    /**
     * Start secure server
     * 
     * @param node
     * @throws IOException
     * @throws KeyStoreException
     */
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
        InetAddress inetAddress = InetAddress.getByName(ip);
        SSLServerSocket serverSocket = (SSLServerSocket) factory.createServerSocket(port, 0, inetAddress);

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

    /**
     * Start insecure server
     * 
     * @param node
     * @throws IOException
     * @throws InterruptedException
     */
    public void serverInsecure(Node node) throws IOException, InterruptedException {
        String ip = node.getIp();
        int port = node.getPort()+1;

        // Create InetAddress from the IP
        InetAddress inetAddress = InetAddress.getByName(ip);

        // Use normal ServerSocket instead of SSLServerSocket, binding to the specific IP address
        ServerSocket sSocket = new ServerSocket(port, 0, inetAddress); //socket novo

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
