package handlers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import client.Node;
import dtos.NodeDTO;

public class NodeThread extends Thread {

    private Node currentNode;

    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private BlockingQueue<String> commandQueue;

    private NodeCommandHandler commandHandler = new NodeCommandHandler();

    public NodeThread (Node currentNode, Socket socket, boolean isServer) {
        this.currentNode = currentNode;
        this.socket = socket;
        this.commandQueue = new LinkedBlockingQueue<>();
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());

            if (isServer) {
                BigInteger hash = (BigInteger) in.readObject(); // Read the hash from the other node
                out.writeObject(currentNode.getHashNumber()); // Send the hash of the current node
                currentNode.getNodeService().addThread(hash, this);
            } else {
                out.writeObject(currentNode.getHashNumber()); // Send the hash of the current node
                BigInteger hash = (BigInteger) in.readObject(); // Read the hash from the other node
                currentNode.getNodeService().addThread(hash, this);
            }

        } catch (Exception e) {
            System.err.println("Error creating input/output streams");
            System.exit(-1);
        }
    }

    public BigInteger getHash() {
        return currentNode.getHashNumber();
    }

    public NodeDTO getNextNodeDTO() {
        return currentNode.getNextNode();
    }

    public void addCommand(String command) {
        commandQueue.add(command);
    }

    /**
     * Function that is called when the thread is started
     */
    public void run() {  // TODO: Change the func to close the connection after the command is sent or received
        while (true) {

            // Check for commands from the queue with a timeout
            String command;
            try {
                command = commandQueue.poll(100, TimeUnit.MILLISECONDS);
                if (command != null) {
                    processCommand(command);
                } else {
                    processCommand("endConection"); // If there are no commands, end the connection
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            // Wait for messages from the input stream
            try {
                if (in.available() > 0) {
                    String message = (String) in.readObject();
                    processCommand(message);
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    // TODO: Create a enumerated type for the commands?
    private void processCommand(String command) {
        if (command.contains("getNodeWithHash:")) { // Get the NodeDTO of the next node of the one with the given hash
            commandHandler.getNodeRequest();
        } else if (command.equals("endConection")) {
            commandHandler.endConectionRequest();
        }
    }

}
