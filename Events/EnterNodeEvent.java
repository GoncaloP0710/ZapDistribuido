package Events;

import java.math.BigInteger;

import Message.*;
import dtos.NodeDTO;

public class EnterNodeEvent extends NodeEvent {

	private NodeDTO toEnter;
	private BigInteger toEnterHash;
    
    public EnterNodeEvent(ChordInternalMessage msg) {
		super(msg);
		this.toEnter = msg.getNodeToEnter();
		this.toEnterHash = toEnter.getHash();
	}

	public NodeDTO getToEnter() {
		return toEnter;
	}

	public BigInteger getToEnterHash() {
		return toEnterHash;
	}
    
}
