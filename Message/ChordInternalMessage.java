package Message;

import java.math.BigInteger;
import java.util.ArrayList;

import dtos.NodeDTO;

public class ChordInternalMessage extends Message {

    private NodeDTO nextNode; // UpdateNeighbors Event
    private NodeDTO previousNode; // UpdateNeighbors Event
    private NodeDTO nodeToEnter; // EnterNode Event
    private NodeDTO nodeToUpdate; // UpdateNodeFingerTableEvent
    private int counter; // UpdateNodeFingerTableEvent
    private ArrayList<NodeDTO> fingerTable; // UpdateNodeFingerTableEvent
    private NodeDTO initializer; // BroadcastUpdateFingerTableEvent
    private boolean finishedBroadcasting; // BroadcastUpdateFingerTableEvent

    // TODO: Check if the receiverHash is necessary - Maybe can only be on the User message

    // UpdateNeighboringNodesEvent
    public ChordInternalMessage(MessageType messageType, NodeDTO nodeToUpdate, BigInteger reciverHash, NodeDTO nextNode, NodeDTO previousNode) {
        super(messageType, reciverHash);
        this.nextNode = nextNode;
        this.previousNode = previousNode;
    }

    // EnterNodeEvent
    public ChordInternalMessage(MessageType messageType, BigInteger reciverHash, NodeDTO nodeToEnter) {
        super(messageType, reciverHash);
        this.nodeToEnter = nodeToEnter;
    }
    
    /**
     * UpdateNodeFingerTableEvent
     * 
     * @param nodeToUpdate this is the that first started the event
     * @requires counter == 0
     */
    public ChordInternalMessage(MessageType messageType, BigInteger reciverHash, NodeDTO nodeToUpdate, int counter) {
        super(messageType, reciverHash);
        this.nodeToUpdate = nodeToUpdate;
        this.counter = counter;
        this.fingerTable = new ArrayList<>();
    }

    /**
     * BroadcastUpdateFingerTableEvent
     * @requires finishedBroadcasting == false
     */
    public ChordInternalMessage(MessageType messageType, BigInteger reciverHash, Boolean finishedBroadcasting, NodeDTO initializer) {
        super(messageType, reciverHash);
        this.initializer = initializer;
        this.finishedBroadcasting = finishedBroadcasting;
    }

    public NodeDTO getNextNode(){
        return this.nextNode;
    }

    public NodeDTO getPreviousNode(){
        return this.previousNode;
    }

    public NodeDTO getNodeToEnter(){
        return this.nodeToEnter;
    }

    public NodeDTO getNodeToUpdate(){
        return this.nodeToUpdate;
    }

    public int getCounter(){
        return this.counter;
    }

    public void incCounter(){
        this.counter++;
    }

    public ArrayList<NodeDTO> getFingerTable(){
        return this.fingerTable;
    }

    public void addNodeToFingerTable(NodeDTO node){
        this.fingerTable.add(node);
    }

    public NodeDTO getInitializer(){
        return this.initializer;
    }

    public boolean getFinishedBroadcasting(){
        return this.finishedBroadcasting;
    }

}
