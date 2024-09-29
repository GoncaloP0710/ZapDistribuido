package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import Events.*;
import Message.ChordInternalMessage;
import Message.Message;
import Message.UserMessage;
import utils.observer.*;

public class NodeThread extends Thread implements Subject<NodeEvent> {

    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private Message msg;
    private Listener<NodeEvent> listener;

    // If command is null, it means that the thread is a server and its objective is to process the command it receives
    public NodeThread (Socket socket, Message msg, Listener<NodeEvent> listener) {
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
    public NodeEvent processCommand(Message messageToProcess) throws ClassNotFoundException, IOException {
        switch (messageToProcess.getMsgType()) {
            case EnterNode:
                emitEvent(new EnterNodeEvent((ChordInternalMessage) messageToProcess));
            case UpdateNeighbors:
                emitEvent(new UpdateNeighboringNodesEvent((ChordInternalMessage) messageToProcess));
            case UpdateFingerTable:
                emitEvent(new UpdateNodeFingerTableEvent((ChordInternalMessage) messageToProcess));
            case SendMsg:
                emitEvent(new NodeSendMessageEvent((UserMessage) messageToProcess));
            default:
                return null;
        }
    }

    @Override
    public void emitEvent(NodeEvent e) {
        this.listener.processEvent(e);
    }

}
