package psd.group4.events;

import psd.group4.dtos.NodeDTO;
import psd.group4.message.ChordInternalMessage;

public class UpdateNeighboringNodesEvent extends NodeEvent {

    private NodeDTO next;
    private NodeDTO previous;
    private NodeDTO initializer;
    
    public UpdateNeighboringNodesEvent(ChordInternalMessage msg) {
		super(msg);
		this.next = msg.getNextNode();
        this.previous = msg.getPreviousNode();
        this.initializer = msg.getInitializer();
	}

    public NodeDTO getNext() {
        return next;
    }

    public NodeDTO getPrevious() {
        return previous;
    }

    public NodeDTO getInitializer() {
        return initializer;
    }
}
