package Events;

import Message.Message;
import dtos.NodeDTO;

public class UpdateNeighboringNodesEvent extends NodeEvent {

    private NodeDTO next;
    private NodeDTO previous;
    
    public UpdateNeighboringNodesEvent(Message msg, NodeDTO next, NodeDTO previous) {
		super(msg);
		this.next = next;
        this.previous = previous;
	}

    public NodeDTO getNext() {
        return next;
    }

    public NodeDTO getPrevious() {
        return previous;
    }
}
