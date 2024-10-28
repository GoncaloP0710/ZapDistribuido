package Client;

import java.util.ArrayList;

import java.net.Socket;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

import Events.*;
import Handlers.EncryptionHandler;
import Handlers.InterfaceHandler;
import Handlers.KeyHandler;
import Message.*;
import Utils.observer.*;
import Utils.*;

public class NodeThread extends Thread implements Subject<NodeEvent> {

    private Socket socket = null;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    private boolean running = true;

    private final Object lock = new Object();

    private BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();
    private Listener<NodeEvent> listener;
    private KeyHandler keyHandler;

    // If msg is null, it means that the thread is a server and its objective is to process the command it receives
    public NodeThread (Socket socket, Message msg, Listener<NodeEvent> listener, KeyHandler keyHandler) {
        addMessage(msg);
        this.socket = socket;
        this.keyHandler = keyHandler;
        this.listener = listener;
        try {
            this.out = new ObjectOutputStream(socket.getOutputStream());
            this.in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            System.err.println("Error creating input/output streams: " + e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
        InterfaceHandler.internalInfo("NodeThread created");
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

    public void addMessage(Message msg) {
        if (msg == null) {
            return;
        }
        messageQueue.offer(msg);
    }
    
    public void removeMessage(Message msg) {
        messageQueue.remove(msg);
    }
    
    public List<Message> getMessages() {
        return new ArrayList<>(messageQueue);
    }   

    private void sendMsg() {
        try {
            InterfaceHandler.internalInfo("Starting sendMsg thread");
            while (running) {
                Message msg = messageQueue.take(); // Wait for messages if the queue is empty
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

    private void reciveMsg() {
        try {
            InterfaceHandler.internalInfo("Waiting for messages to recive and send to the processMessages");
            while (running) {
                Message messageToProcess = (Message) in.readObject();
                new Thread(() -> processMessages(messageToProcess)).start();
            }
        } catch (EOFException e) {
            InterfaceHandler.erro("Connection closed");
            return; // Exit the loop if the end of the stream is reached
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            InterfaceHandler.erro("Error receiving message");
        }
    }

    private void processMessages(Message messageToProcess) {
        try {
            InterfaceHandler.internalInfo("Processing message: " + messageToProcess.getMsgType());
            processCommand(messageToProcess);
        } catch (Exception e) {
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
