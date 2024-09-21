package handlers;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import client.Node;

public class NodeThread extends Thread {

    Node currentNode;

    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    public NodeThread (Node currentNode, Socket socket) {
        this.currentNode = currentNode;
        this.socket = socket;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.err.println("Error creating input/output streams");
            System.exit(-1);
        }
    }

    /**
     * Function that is called when the thread is started
     */
    public void run() {
        // TODO: Wait for messages from the other node
    }

}
