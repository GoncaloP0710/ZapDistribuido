package psd.group4.events;

import java.math.BigInteger;

import psd.group4.dtos.NodeDTO;
import psd.group4.message.ChordInternalMessage;

public class RemoveSharedKeyEvent extends NodeEvent {

    private NodeDTO initializer;
    private BigInteger target;

    public RemoveSharedKeyEvent(ChordInternalMessage msg) {
        super(msg);
        this.initializer = msg.getInitializer();
        this.target = msg.getTarget();
    }

    public NodeDTO getInitializer() {
        return initializer;
    }

    public BigInteger getTarget() {
        return target;
    }
}