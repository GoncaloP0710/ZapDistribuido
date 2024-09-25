package Events;

import client.Node;
import dtos.NodeDTO;

public class EnterNodeEvent extends NodeEvent {

	private NodeDTO toEnter;
    
    public EnterNodeEvent(Node current, NodeDTO toEnter) {
		super(current);
		this.toEnter = toEnter;
	}
    
}
