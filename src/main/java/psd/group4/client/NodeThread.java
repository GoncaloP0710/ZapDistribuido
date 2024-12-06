package psd.group4.client;

import java.util.ArrayList;

import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.List;

import psd.group4.handlers.InterfaceHandler;
import psd.group4.utils.observer.*;
import psd.group4.events.*;
import psd.group4.message.*;

public class NodeThread extends Thread implements Subject<NodeEvent> {

    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private boolean running = true;

    private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private Listener<NodeEvent> listener;

    // If msg is null, it means that the thread is a server and its objective is to process the command it receives
    public NodeThread (Socket socket, Message msg, Listener<NodeEvent> listener) {
        addMessage(msg);
        this.socket = socket;
        this.listener = listener;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.err.println("Error creating input/output streams: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Function that is called when the thread is started
     */
    public void run() {
        new Thread(this::sendMsg).start();
        new Thread(this::reciveMsg).start();
    }

    public void endThread() {
        running = false;
        try {
            this.socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Add a message to the queue
     * 
     * @param msg
     */
    public void addMessage(Message msg) {
        if (msg == null) {
            return;
        }
        messageQueue.offer(msg);
    }
    
    /**
     * Remove a message from the queue
     * 
     * @param msg
     */
    public void removeMessage(Message msg) {
        messageQueue.remove(msg);
    }
    
    public List<Message> getMessages() {
        return new ArrayList<>(messageQueue);
    }   

    /**
     * Send messages to the node receiver
     */
    private void sendMsg() {
        try {
            while (running) {
                Message msg = messageQueue.take(); // Wait for messages if the queue is empty
                InterfaceHandler.internalInfo("Sending message: " + msg.getMsgType());
                out.writeObject(msg); // Send the message to the node receiver
            }
        } catch (IOException | InterruptedException e) {
            if (running) {
                e.printStackTrace();
            } else {
                System.out.println("Thread interrupted, exiting sendMsg loop.");
            }
        }
    }

    /**
     * Receive messages from the node sender and sends them to the processMessages function
     */
    private void reciveMsg() {
        try {
            while (running) {
                Message messageToProcess = (Message) in.readObject();
                new Thread(() -> processMessages(messageToProcess)).start();
            }
        } catch (EOFException e) {
            try {
                socket.close();
            } catch (IOException e1) {
                InterfaceHandler.erro("Error closing socket");
            }
            InterfaceHandler.info("Connection closed");
            return; // Exit the loop if the end of the stream is reached
        } catch (ClassNotFoundException | IOException e) {
            try {
                socket.close();
            } catch (IOException e1) {
                InterfaceHandler.erro("Error closing socket");
            }
            InterfaceHandler.info("Error receiving message");
        }
    }

    /**
     * Process the messages received
     * 
     * @param messageToProcess
     */
    private void processMessages(Message messageToProcess) {
        try {
            InterfaceHandler.internalInfo("Processing message: " + messageToProcess.getMsgType());
            processCommand(messageToProcess);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Transform the command on a NodeEvent and emit it
     * 
     * @param command
     * @return
     * @throws IOException 
     * @throws ClassNotFoundException 
     * @throws NoSuchAlgorithmException 
     */
    public void processCommand(Message messageToProcess) throws ClassNotFoundException, IOException, NoSuchAlgorithmException {
        switch (messageToProcess.getMsgType()) {
            case EnterNode:
                emitEvent(new EnterNodeEvent((ChordInternalMessage) messageToProcess));
                break;
            case UpdateNeighbors:
                emitEvent(new UpdateNeighboringNodesEvent((ChordInternalMessage) messageToProcess));
                break;
            case UpdateFingerTable:
                emitEvent(new UpdateNodeFingerTableEvent((ChordInternalMessage) messageToProcess));
                break;
            case broadcastUpdateFingerTable:
                emitEvent(new BroadcastUpdateFingerTableEvent((ChordInternalMessage) messageToProcess));
                break;
            case SendMsg:
                emitEvent(new NodeSendMessageEvent((UserMessage) messageToProcess));
                break;
            case addCertificateToTrustStore:
                emitEvent(new AddCertificateToTrustStoreEvent((ChordInternalMessage) messageToProcess));
                break;
            case diffHellman:
                emitEvent(new DiffHellmanEvent((ChordInternalMessage) messageToProcess));
                break;
            case Notify:
                emitEvent(new NotifyEvent((ChordInternalMessage) messageToProcess));
                break;
            case RemoveSharedKey:
                emitEvent(new RemoveSharedKeyEvent((ChordInternalMessage) messageToProcess));
                break;
            case SendGroupMsg:
                emitEvent(new NodeSendGroupMessageEvent((UserMessage) messageToProcess));
                break;
            case AddUserToGroup:
                emitEvent(new AddUserToGroupEvent((ChordInternalMessage) messageToProcess));
                break;
            default:
                break;
        }
        return;
    }

    @Override
    public void emitEvent(NodeEvent e) {
        this.listener.processEvent(e);
    }

}
