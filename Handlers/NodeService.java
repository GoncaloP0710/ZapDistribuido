package handlers;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

import client.Node;
import handlers.NodeThread;

/**
 * This class is responsible for the node comunication on the network
 */
public class NodeService {

    private Node currentNode;

    // ---------------------- Verify if this is needed ----------------------
    private String ip;
    private int port;
    // -----------------------------------------------------------------------

    String keystoreFile;
    String keystorePassword;

    private static SSLServerSocket serverSocket;
    public static List<NodeThread> activeThreads = Collections.synchronizedList(new ArrayList<>());

    public NodeService(Node currentNode, String ip, int port, String keystoreFile, String keystorePassword) {
        this.currentNode = currentNode;
        this.ip = ip;
        this.port = port;
        this.keystoreFile = keystoreFile;
        this.keystorePassword = keystorePassword;
    }

    public void startServer() throws IOException {

        System.setProperty("javax.net.ssl.keyStore", keystoreFile);
        System.setProperty("javax.net.ssl.keyStorePassword", keystorePassword);
        System.setProperty("javax.net.ssl.keyStoreType", "JCEKS");

        // Get an SSLSocketFactory from the SSLContext
        ServerSocketFactory factory = SSLServerSocketFactory.getDefault();
        SSLServerSocket sslServerSocket = (SSLServerSocket) factory.createServerSocket(port);

        while (true) {
            Socket clientSocket = null; // other node socket
            try {
                clientSocket = serverSocket.accept();
                NodeThread newServerThread = new NodeThread(currentNode, clientSocket);
                activeThreads.add(newServerThread);
                newServerThread.start();

            } catch (IOException e) {
                System.err.println(e.getMessage());
                System.exit(-1);
            }

        }
    }

}
