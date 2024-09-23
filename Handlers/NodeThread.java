package handlers;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import client.Node;
import dtos.NodeDTO;
import handlers.NodeCommandHandler;

public class NodeThread extends Thread {

    private Node currentNode;

    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private String command;
    private NodeCommandHandler commandHandler;

    // If command is null, it means that the thread is a server and its objective is to process the command it receives
    public NodeThread (Node currentNode, Socket socket, String command) {
        this.currentNode = currentNode;
        this.socket = socket;
        commandHandler = new NodeCommandHandler(this);
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.err.println("Error creating input/output streams");
            System.exit(-1);
        }
    }

    public Socket getSocket() {
        return socket;
    }

    /**
     * Function that is called when the thread is started
     */
    public void run() {
        if (command != null) {
            try {
                out.writeObject(command);
                out.writeObject("End Connection");
                commandHandler.processCommand("End Connection");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                String commandToProcess = (String) in.readObject();
                commandHandler.processCommand(commandToProcess);
                commandToProcess = (String) in.readObject(); // Should be a "End Connection"
                commandHandler.processCommand(commandToProcess);
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }

}
