package Message;

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
    private NodeDTO senderDto; // BroadcastUpdateFingerTableEvent
    private boolean finishedBroadcasting; // BroadcastUpdateFingerTableEvent

    // UpdateNeighboringNodesEvent
    public ChordInternalMessage(MessageType messageType, NodeDTO nextNode, NodeDTO previousNode) {
        super(messageType);
        this.nextNode = nextNode;
        this.previousNode = previousNode;
    }

    // EnterNodeEvent
    public ChordInternalMessage(MessageType messageType, NodeDTO nodeToEnter) {
        super(messageType);
        this.nodeToEnter = nodeToEnter;
    }
    
    /**
     * UpdateNodeFingerTableEvent
     * 
     * @param nodeToUpdate this is the that first started the event
     * @requires counter == 0
     */
    public ChordInternalMessage(MessageType messageType, NodeDTO nodeToUpdate, int counter) {
        super(messageType);
        this.nodeToUpdate = nodeToUpdate;
        this.counter = counter;
        this.fingerTable = new ArrayList<>();
    }

    /**
     * BroadcastUpdateFingerTableEvent
     * @requires finishedBroadcasting == false
     */
    public ChordInternalMessage(MessageType messageType, Boolean finishedBroadcasting, NodeDTO initializer, NodeDTO senderDto) {
        super(messageType);
        this.initializer = initializer;
        this.senderDto = senderDto;
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

    public NodeDTO getSenderDto(){
        return this.senderDto;
    }

    public void setSenderDto(NodeDTO senderDto){
        this.senderDto = senderDto;
    }

    public boolean getFinishedBroadcasting(){
        return this.finishedBroadcasting;
    }

    @Override
    public String toString() {
        return "ChordInternalMessage{" +
                "nextNode=" + nextNode +
                ", previousNode=" + previousNode +
                ", nodeToEnter=" + nodeToEnter +
                ", nodeToUpdate=" + nodeToUpdate +
                ", counter=" + counter +
                ", fingerTable=" + fingerTable +
                ", initializer=" + initializer +
                ", finishedBroadcasting=" + finishedBroadcasting +
                '}';
    }

}
