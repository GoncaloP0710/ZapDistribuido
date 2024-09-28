package Message;

import java.math.BigInteger;

import dtos.NodeDTO;
import dtos.UserDTO;

public class ChordInternalMessage extends Message {

    private NodeDTO nextNode; // UpdateNeighbors Msg
    private NodeDTO previousNode; // UpdateNeighbors Msg
    // ChordInternalMessage(MessageType, NodeDTO, null, null, NodeDTO)
    // TODO: Remove reciverHash and senderDTO from constructor if not needed
    public ChordInternalMessage(MessageType messageType, UserDTO senderDTO, 
        BigInteger reciverHash, NodeDTO nextNode, NodeDTO previousNode) {

        super(messageType, senderDTO, reciverHash);
        this.nextNode = nextNode;
        this.previousNode = previousNode;
    }

    public NodeDTO getNextNode(){
        return this.nextNode;
    }

    public NodeDTO getPreviousNode(){
        return this.previousNode;
    }
}
