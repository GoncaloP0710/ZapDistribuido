package Message;

import java.math.BigInteger;

import dtos.NodeDTO;
import dtos.UserDTO;

public class ChordInternalMessage extends Message {

    private NodeDTO nextNode; // UpdateNeighbors
    private NodeDTO previousNode; // UpdateNeighbors

    public ChordInternalMessage(MessageType messageType, UserDTO senderDTO, 
        BigInteger reciverHash, NodeDTO nextNode, NodeDTO previousNode) {

        super(messageType, senderDTO, reciverHash);
        this.nextNode = nextNode;
        this.previousNode = previousNode;
    }
}
