package psd.group4.events;

import java.math.BigInteger;

import psd.group4.dtos.NodeDTO;
import psd.group4.message.*;

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
