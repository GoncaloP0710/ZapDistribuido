package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import Events.EnterNodeEvent;
import Events.NodeEvent;
import Message.Message;
import dtos.NodeDTO;
import utils.observer.*;
import utils.*;

public class NodeThread extends Thread implements Subject<NodeEvent> {

    private Node currentNode; // TODO: If in the end the node is not necessary, we should remove it

    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private Message msg;
    private Listener<NodeEvent> listener;

    // If command is null, it means that the thread is a server and its objective is to process the command it receives
    public NodeThread (Node currentNode, Socket socket, Message msg, Listener<NodeEvent> listener) {
        this.currentNode = currentNode;
        this.socket = socket;
        this.msg = msg;
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
        if (msg != null) {
            try {
                out.writeObject(msg);
                out.writeObject("End Connection");
                endThread();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Message messageToProcess = (Message) in.readObject();
                NodeEvent event = processCommand(messageToProcess);
                emitEvent(event);
                String commandToProcess = (String) in.readObject(); // "End Connection"
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
    public NodeEvent processCommand(Message messageToProcess) throws ClassNotFoundException, IOException { // TODO: Add more types if necessary
        switch (messageToProcess.getMsgType()) {
            case EnterNode:
                // TODO: return the respective event
            default:
                return null;
        }
    }

    @Override
    public void emitEvent(NodeEvent e) {
        this.listener.processEvent(e);
    }

}
