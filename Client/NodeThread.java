package Client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;

import Events.*;
import Message.ChordInternalMessage;
import Message.Message;
import Message.UserMessage;
import Utils.observer.*;

public class NodeThread extends Thread implements Subject<NodeEvent> {

    private final ReentrantLock lock = new ReentrantLock();
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
                System.out.println("Message sent: " + msg.toString());
                endThread();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                Message messageToProcess = (Message) in.readObject();
                System.out.println("Message received: " + messageToProcess.toString());
                processCommand(messageToProcess);
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
    public void processCommand(Message messageToProcess) throws ClassNotFoundException, IOException {
        lock.lock();
        try {
        switch (messageToProcess.getMsgType()) {
            case EnterNode:
                System.out.println("EnterNode event to process"); 
                emitEvent(new EnterNodeEvent((ChordInternalMessage) messageToProcess));
                break;
            case UpdateNeighbors:
                System.out.println("UpdateNeighbors event to process");
                emitEvent(new UpdateNeighboringNodesEvent((ChordInternalMessage) messageToProcess));
                break;
            case UpdateFingerTable:
                System.out.println("UpdateFingerTable event to process");
                emitEvent(new UpdateNodeFingerTableEvent((ChordInternalMessage) messageToProcess));
                break;
            case broadcastUpdateFingerTable:
                System.out.println("BroadcastUpdateFingerTable event to process");
                emitEvent(new BroadcastUpdateFingerTableEvent((ChordInternalMessage) messageToProcess));
                break;
            case SendMsg:
                emitEvent(new NodeSendMessageEvent((UserMessage) messageToProcess));
                break;
            default:
                break;
        }
        } finally {
            lock.unlock();
        }
        return;
    }

    @Override
    public void emitEvent(NodeEvent e) {
        this.listener.processEvent(e);
    }

}
