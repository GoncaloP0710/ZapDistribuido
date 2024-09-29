package Events;

import Message.ChordInternalMessage;
import dtos.NodeDTO;

public class UpdateNeighboringNodesEvent extends NodeEvent {

    private NodeDTO next;
    private NodeDTO previous;
    
    public UpdateNeighboringNodesEvent(ChordInternalMessage msg) {
		super(msg);
		this.next = msg.getNextNode();
        this.previous = msg.getPreviousNode();
	}

    public NodeDTO getNext() {
        return next;
    }

    public NodeDTO getPrevious() {
        return previous;
    }
}
