package Message;

import java.math.BigInteger;

import client.Node;
import dtos.NodeDTO;
import dtos.UserDTO;

public class ChordInternalMessage extends Message { // TODO: Implement Add the UpdateNodeFingerTableEvent

    private NodeDTO nextNode; // UpdateNeighbors Event
    private NodeDTO previousNode; // UpdateNeighbors Event
    private NodeDTO nodeToEnter; // EnterNode Event

    // UpdateNeighboringNodesEvent
    public ChordInternalMessage(MessageType messageType, UserDTO senderDTO, BigInteger reciverHash, NodeDTO nextNode, NodeDTO previousNode) {
        super(messageType, senderDTO, reciverHash);
        this.nextNode = nextNode;
        this.previousNode = previousNode;
    }

    // EnterNodeEvent
    public ChordInternalMessage(MessageType messageType, BigInteger reciverHash, UserDTO senderDTO, NodeDTO nodeToEnter) {
        super(messageType, senderDTO, reciverHash);
        this.nodeToEnter = nodeToEnter;
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

}
