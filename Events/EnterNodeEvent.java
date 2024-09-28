package Events;

import Message.Message;
import client.Node;
import dtos.NodeDTO;

public class EnterNodeEvent extends NodeEvent {

	private NodeDTO toEnter;
    
    public EnterNodeEvent(Message msg, NodeDTO toEnter) {
		super(msg);
		this.toEnter = toEnter;
	}

	public NodeDTO getToEnter() {
		return toEnter;
	}
    
}
