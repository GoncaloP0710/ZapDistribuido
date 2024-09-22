package handlers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import client.Node;

public class NodeThread extends Thread {

    Node currentNode;

    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private BlockingQueue<String> commandQueue;

    public NodeThread (Node currentNode, Socket socket) {
        this.currentNode = currentNode;
        this.socket = socket;
        this.commandQueue = new LinkedBlockingQueue<>();
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
        while (true) {
            // Check for commands from the queue with a timeout
            String command;
            try {
                command = commandQueue.poll(100, TimeUnit.MILLISECONDS);

                if (command != null) {
                    processCommand(command);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Wait for messages from the input stream
            try {
                if (in.available() > 0) {
                    Object message = in.readObject();
                    // Process the message
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processCommand(String command) {

    }

}
