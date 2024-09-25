package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import Events.EnterNodeEvent;
import Events.NodeEvent;
import dtos.NodeDTO;
import utils.observer.Listener;
import utils.observer.Subject;

public class NodeThread extends Thread implements Subject<NodeEvent> {

    private Node currentNode; // TODO: If in the end the node is not necessary, we should remove it

    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private String command;
    private Listener<NodeEvent> listener;

    // If command is null, it means that the thread is a server and its objective is to process the command it receives
    public NodeThread (Node currentNode, Socket socket, String command, Listener<NodeEvent> listener) {
        this.currentNode = currentNode;
        this.socket = socket;
        this.command = command;
        setListener(listener);
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.err.println("Error creating input/output streams");
            System.exit(-1);
        }
    }

    public void setListener (Listener<NodeEvent> listener) {
        this.listener = listener;
    }

    /**
     * Function that is called when the thread is started
     */
    public void run() {
        if (command != null) {
            try {
                out.writeObject(command);
                out.writeObject("End Connection");
                endThread();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                String commandToProcess = (String) in.readObject();
                NodeEvent event = processCommand(commandToProcess);
                emitEvent(event);
                // Should be a "End Connection"
                commandToProcess = (String) in.readObject();
                endThread();
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void endThread() {
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Transform the command on a NodeEvent
     * 
     * @param command
     * @return
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    public NodeEvent processCommand(String command) throws ClassNotFoundException, IOException { // TODO: Add more commands if necessary
        switch (command) {
            case "Enter Node":
                NodeDTO toEnter = (NodeDTO) in.readObject(); // recive the node to enter
                return new EnterNodeEvent(currentNode, toEnter);
            default:
                return null;
        }
    }

    @Override
    public void emitEvent(NodeEvent e) {
        this.listener.processEvent(e);
    }

}
